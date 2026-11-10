package org.beatengine.mcpgateway.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WSConfig implements WebSocketConfigurer {

    public final static String AUTH_HEADER_KEY = "Authorization";
    public final static String AUTH_STATE = "AUTH_STATE";
    public static int WEBSOCKET_SESSION_LIFETIME_SECONDS = 60 * 60 * 24;

    @Value("${server.port}")
    private String serverPort;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebsocketHandlerMcpGatewayRequests(), "/ws").
                addInterceptors(new CookieHandshakeInterceptor())
                .setAllowedOrigins("*");
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                "Added Websocket handler /ws");

        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                "URL: ws://localhost:"+serverPort+"/ws");
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                "API-DOC: http://localhost:"+serverPort+"/springwolf/docs");
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                "API-UI: http://localhost:"+serverPort+"/springwolf/asyncapi-ui.html");

        new Thread(()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                    "Info-Page McpGateway, code-demo: http://localhost:"+serverPort+"/");
        }).start();

        final String mcpGatewasWsSessionLifetimeSeconds = System.getProperty("MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS");
        try {
            final Integer i = Integer.valueOf(mcpGatewasWsSessionLifetimeSeconds);
            WEBSOCKET_SESSION_LIFETIME_SECONDS = i;
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO, "MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS="+i);
        }
        catch (Exception e)
        {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,
                    "MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS NOT SET (default 24h)");
        }


    }
}
