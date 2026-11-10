package org.beatengine.mcpgateway.mcp;

import io.modelcontextprotocol.client.McpSyncClient;

import java.time.Instant;

public class RegisteredMcpServer {
    private final String name;
    private final String url;
    private final McpSyncClient mcpClient; // Connection to the "server"
    private Instant lastSeen;

    public RegisteredMcpServer(String name, String url, McpSyncClient mcpClient) {
        this.name = name;
        this.url = url;
        this.mcpClient = mcpClient;
        this.lastSeen = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastSeen = Instant.now();
    }

    // Getter
    public String getName() { return name; }
    public String getUrl() { return url; }
    public McpSyncClient getMcpClient() { return mcpClient; }
    public Instant getLastSeen() { return lastSeen; }
}
