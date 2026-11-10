package org.beatengine.mcpgateway.websocket;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.beatengine.mcpgateway.McpGatewayApplication;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CookieHandshakeInterceptor implements HandshakeInterceptor {
    /***
     * Inject the HTTP Header Cookie to the websocket handler attributes as part of session management
     * @param request the current request
     * @param response the current response
     * @param wsHandler the target WebSocket handler
     * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
     * session; the provided attributes are copied, the original map is not used.
     * @return true
     * @throws Exception
     */

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        final WebsocketHandlerMcpGatewayRequests handler = (WebsocketHandlerMcpGatewayRequests)((ExceptionWebSocketHandlerDecorator)wsHandler).getLastHandler();
        String userSession = null;
        if (request instanceof ServletServerHttpRequest) {
            final HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            final Cookie[] cookies = servletRequest.getCookies();
            if(cookies != null) {
                Optional<Cookie> any = Arrays.stream(cookies).filter(c -> WSConfig.AUTH_HEADER_KEY.equals(c.getName())).findAny();
                if (any.isPresent()) {
                    final Cookie cookie = any.get();
                    userSession = cookie.getValue();
                    Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "UserSession Cookie: " + cookie.getValue());
                }
            }


        }
        if (response instanceof ServletServerHttpResponse) {
            final HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            //If the user is not already known, register a new
            if(!handler.hasUserSession(userSession)) {
                userSession = handler.generateUserSessionId();
                final Cookie myCookie = new Cookie(WSConfig.AUTH_HEADER_KEY, userSession);
                myCookie.setPath("/");
                myCookie.setHttpOnly(true);
                myCookie.setMaxAge(WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS);
                myCookie.setSecure(true);
                servletResponse.addCookie(myCookie);
            }
        }

        if(userSession != null)
        {
            //Set the cookie-user-session for usage in ws-session attributes
            attributes.put(WSConfig.AUTH_HEADER_KEY, new UserSession(userSession, WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS));
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
