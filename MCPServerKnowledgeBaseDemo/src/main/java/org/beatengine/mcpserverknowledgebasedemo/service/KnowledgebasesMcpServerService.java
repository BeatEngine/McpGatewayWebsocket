package org.beatengine.mcpserverknowledgebasedemo.service;

import org.beatengine.mcpserverknowledgebasedemo.tools.Tool1;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Configuration
public class KnowledgebasesMcpServerService {

    private static int HTTP_TIMEOUT_SECONDS = 30;

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public ToolCallbackProvider registerMyTools() {
        return ToolCallbackProvider.from(ToolCallbacks.from(new Tool1()));
    }



    public KnowledgebasesMcpServerService()
    {
        new Thread(()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Logger.getLogger(KnowledgebasesMcpServerService.class.getSimpleName()).log(Level.INFO, "Stated MCP server " + serverName + " on localhost:" + serverPort);
        }).start();
    }

    /*private McpStatelessSyncServer buildNewServer(final String url)
    {
        final JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JacksonUtils.getDefaultJsonMapper());
        // Initialize HTTP transport for MCP server
        final HttpServletStatelessServerTransport transport = HttpServletStatelessServerTransport
                .builder()
                .messageEndpoint(CUSTOM_MESSAGE_ENDPOINT)
                .jsonMapper(jsonMapper)
                //.contextExtractor(new McpTransportContextExtractor<HttpServletRequest>() {
                //    @Override
                //    public McpTransportContext extract(HttpServletRequest request) {
                //        return null;
                //    }
                //})
                .build();

        // Build synchronous MCP server
        final McpStatelessSyncServer server = McpServer.sync(transport)
                .serverInfo(new McpSchema.Implementation("1.0.0", "Demo MCP Server 1.0.0"))
                .requestTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)
                        .prompts(false)
                        .tools(true)
                        .logging()
                        .completions()
                        .build())
                //.tools(ToolLoader.getToolSpecs(List.of(new Tool1())))
                .build();

        Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO,"MCP initialization successful");

        return server;
    }
    */

}
