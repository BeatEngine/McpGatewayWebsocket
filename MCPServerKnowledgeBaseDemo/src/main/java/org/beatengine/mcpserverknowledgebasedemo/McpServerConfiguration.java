package org.beatengine.mcpserverknowledgebasedemo;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfiguration {


    private static String CUSTOM_MESSAGE_ENDPOINT = "/mcp/sse";

    public McpServerConfiguration()
    {
        McpServerKnowledgeBaseDemoApplication.loadRootProjectEnvironmentVariables();
        //CUSTOM_MESSAGE_ENDPOINT = System.getProperty("CUSTOM_MESSAGE_ENDPOINT", CUSTOM_MESSAGE_ENDPOINT); !!!PostMapping!!!
    }

    @Bean
    public HttpServletStatelessServerTransport mcpStatelessServerTransport()
    {
        final JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JacksonUtils.getDefaultJsonMapper());
        // Initialize HTTP transport for MCP server
        return HttpServletStatelessServerTransport
                .builder()
                .messageEndpoint(CUSTOM_MESSAGE_ENDPOINT)
                .jsonMapper(jsonMapper)
                .build();
    }

}
