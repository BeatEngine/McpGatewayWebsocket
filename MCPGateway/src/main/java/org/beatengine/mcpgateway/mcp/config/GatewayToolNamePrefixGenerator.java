package org.beatengine.mcpgateway.mcp.config;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.stereotype.Component;

@Component
public class GatewayToolNamePrefixGenerator implements McpToolNamePrefixGenerator {

    @Override
    public String prefixedToolName(McpConnectionInfo connectionInfo, McpSchema.Tool tool) {
        final String serverName = connectionInfo.initializeResult().serverInfo().name();
        return serverName + "_" + tool.name();
    }
}
