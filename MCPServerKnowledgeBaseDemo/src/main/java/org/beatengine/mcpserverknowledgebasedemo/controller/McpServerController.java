package org.beatengine.mcpserverknowledgebasedemo.controller;

import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class McpServerController {

    @Autowired
    private HttpServletStatelessServerTransport transport;


    public McpServerController() {

    }

    // SSE-Endpoint
    @PostMapping("/mcp/sse")
    public void handleSse(HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.transport.service(request, response);
    }

}
