package org.beatengine.mcpgateway.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.beatengine.mcpgateway.McpGatewayApplication;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class manages all the <b>MCP-Servers</b> (e.g. from the list <code>MCP_GATEWAY_INITAL_MCP_SERVERS</code>)  (that offers tools over the MCP-Protocol)
 * The <code>AIChatClientService</code> uses this available tools on prompt .tools(McpServerManagerInstance.<code>getToolCallbacks()</code>)
 * The prompt requests come from the <code>GatewayMessageProcessorService</code> (over websocket) in form of <code>WebsocketMessageType.AI_REQUEST</code>
 */

@Service
@EnableScheduling
public class McpServerManager {

    private static int HTTP_TIMEOUT_SECONDS_CONNECT = 5;
    private static int HTTP_TIMEOUT_SECONDS_TOOL_REQUEST = 180;
    private static int HTTP_TIMEOUT_SECONDS_GENERAL_RECONNECT = 86400;

    @Value("${server.port}")
    private String serverPort;


    private final Set<String> knownServerNamesAndUrls = new ConcurrentSkipListSet<>();

    private final Map<String, RegisteredMcpServer> activeServers = new ConcurrentHashMap<>();

    private Tuple<String, String> readNameAndUrlFromProperty(final String propertyValue)
    {
        String name = null;
        String url = null;
        int c = 0;
        for (final String s : propertyValue.split("@")) {
            if(c == 0)
            {
                name = s;
            }
            else
            {
                url = s;
                break;
            }
            c++;
        }
        if(name == null || url == null)
        {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE, "Wrong format environment variable " +
                    "MCP_GATEWAY_INITAL_MCP_SERVERS\nSchema: MCP_GATEWAY_INITAL_MCP_SERVERS=name@url,name2@url2....");
        }
        return new Tuple<String, String>(name, url);
    }

    private void initEnvironmentMcpServers()
    {
        final String mcpGatewayInitalMcpServers = System.getProperty("MCP_GATEWAY_INITAL_MCP_SERVERS", "");
        if(mcpGatewayInitalMcpServers.contains(","))
        {
            for (String s : mcpGatewayInitalMcpServers.split(",")) {
                if(s.contains("@")) {
                    knownServerNamesAndUrls.add(s);
                }
            }
        }
        else if(!mcpGatewayInitalMcpServers.isBlank() && mcpGatewayInitalMcpServers.contains("@"))
        {
            knownServerNamesAndUrls.add(mcpGatewayInitalMcpServers);
        }
        Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO, "Loaded " + knownServerNamesAndUrls.size()
                + " MCP servers from environment\nSchema: MCP_GATEWAY_INITAL_MCP_SERVERS=name@url[,name2@url2...]");
    }

    public McpServerManager()
    {
        initEnvironmentMcpServers();
    }


    private McpSyncClient buildNewClient(final String url) throws Exception
    {

        // Initialize HTTP transport for MCP server
        final HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(url)
                .endpoint(new URL(url).getPath())
                .clientBuilder(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS_CONNECT)))
                .build();
        // Build synchronous MCP client
        final McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("1.0.0", "Enhanced MCP Client 1.0.0"))
                .requestTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS_TOOL_REQUEST))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .build();

        Logger.getLogger(McpServerManager.class.getSimpleName()).log(McpGatewayApplication.FINE,"Initializing MCP client...");
        McpSchema.InitializeResult initResult = client.initialize();
        Logger.getLogger(McpServerManager.class.getSimpleName()).log(McpGatewayApplication.FINE,"MCP initialization successful");

        return client;
    }

    //todo how will the server be detected? --> offer /mcp/register http
    public boolean registerOrUpdateServer(String name, String url) {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "registerOrUpdateServer("+name+", "+url+")");
        if (activeServers.containsKey(name)) {
            activeServers.get(name).updateHeartbeat();
            return true;
        } else {
            try {

                final McpSyncClient mcpClient = buildNewClient(url);

                final RegisteredMcpServer newServer = new RegisteredMcpServer(name, url, mcpClient);
                activeServers.put(name, newServer);
                Logger.getLogger(McpServerManager.class.getSimpleName()).log(McpGatewayApplication.INFO,
                        "Registered MCP Server '" + name + "'");
                return true;
            } catch (Exception e) {
                Logger.getLogger(McpServerManager.class.getSimpleName()).log(Level.WARNING,
                        "Registering MCP Server ("+url+") error: " + e.getMessage());
            }
        }
        return false;
    }


    public List<ToolCallback> getToolCallbacks() {
        final List<ToolCallback> allCallbacks = new ArrayList<>(); //todo cache it an update / inti on event (lost server, add server ...)

        for (RegisteredMcpServer server : activeServers.values()) {
            try {
                final McpSyncClient client = server.getMcpClient();
                final McpSchema.ListToolsResult listToolsResult = client.listTools();
                for(McpSchema.Tool tool : listToolsResult.tools())
                {
                    allCallbacks.add(SyncMcpToolCallback.builder().mcpClient(client).tool(tool).build());
                }
            } catch (Exception e) {
                Logger.getLogger(McpServerManager.class.getSimpleName()).log(Level.WARNING,
                        "Query MCP Server tools error: " + e.getMessage());
            }
        }
        return allCallbacks;
    }

    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    public void registerKnownServers()
    {
        for (final String nameAndUrl : knownServerNamesAndUrls) {
            final Tuple<String, String> tuple = readNameAndUrlFromProperty(nameAndUrl);
            if (registerOrUpdateServer(tuple._1(), tuple._2())) {
                knownServerNamesAndUrls.remove(nameAndUrl);
            }
        }
    }

    @Scheduled(fixedRate = 10000)
    public void cleanupDeadServers() {
        Instant threshold = Instant.now().minus(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS_GENERAL_RECONNECT));

        activeServers.entrySet().removeIf(entry -> {
            boolean timedOut = entry.getValue().getLastSeen().isBefore(threshold);
            if (timedOut) {
                try {
                    entry.getValue().getMcpClient().close();
                }catch (Exception e) {}
                final String name = entry.getKey();
                final String url = entry.getValue().getUrl();
                knownServerNamesAndUrls.add(name+"@"+url);
                Logger.getLogger(McpServerManager.class.getSimpleName()).log(Level.INFO,
                "MCP Server '" + name + "' timed out!");
            }
            return timedOut;
        });
    }

}
