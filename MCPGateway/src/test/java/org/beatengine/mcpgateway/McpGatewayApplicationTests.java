package org.beatengine.mcpgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Level;

@SpringBootTest
public class McpGatewayApplicationTests {

    public final String MCP_GATEWAY_ACCESS_TOKENS = "access-token-McpGatewayApplicationTests";

    public McpGatewayApplicationTests()
    {
        // All logs
        McpGatewayApplication.FINER = Level.INFO;
        McpGatewayApplication.FINE = Level.INFO;

        McpGatewayApplication.loadRootProjectEnvironmentVariables();
        System.setProperty("OLLAMA_URL", "http://localhost:11434");
        System.setProperty("MCP_GATEWAY_ACCESS_TOKENS",MCP_GATEWAY_ACCESS_TOKENS);
    }

    @Test
    void contextLoads() {
    }

}
