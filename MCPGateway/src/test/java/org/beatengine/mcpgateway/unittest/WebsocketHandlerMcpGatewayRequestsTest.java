package org.beatengine.mcpgateway.unittest;


import org.beatengine.mcpgateway.service.GatewayMessageProcessorService;
import org.beatengine.mcpgateway.websocket.*;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.socket.*;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class WebsocketHandlerMcpGatewayRequestsTest {

    private WebsocketHandlerMcpGatewayRequests handler;

    private GatewayMessageProcessorService processorService;

    private final String userSessionId = "test-user-session-93zr98h43";
    private final String accessToken = "WebsocketHandlerMcpGatewayRequestsTest-h7f8sef877sewe8fz";


    public WebsocketHandlerMcpGatewayRequestsTest()
    {
        WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS = 4;
        System.setProperty("MCP_GATEWAY_ACCESS_TOKENS", accessToken);
        try {
            // SINGLETON preparation
            processorService = new GatewayMessageProcessorService();
        } catch (NoSuchAlgorithmException e) {
        }
    }

    private WebSocketSession getDummySession()
    {
        return new DummySession(
                HttpHeaders.EMPTY, Map.of(WSConfig.AUTH_HEADER_KEY,
                new UserSession(userSessionId, WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS))
        );
    }

    private WebSocketSession getEmptyDummySession()
    {
        return new DummySession(
                HttpHeaders.EMPTY, Map.of()
        );
    }

    private String extractExpiry(WebSocketSession s) {
        Object o = s.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o instanceof UserSession u)
        {
            return u.getExpiry().format(DateTimeFormatter.ISO_DATE_TIME);
        }
        return "WSConfig.AUTH_HEADER_KEY not found (user-session)";
    }

    private void resetHandler()
    {
        handler = new WebsocketHandlerMcpGatewayRequests();
        try {
            handler.afterConnectionEstablished(getEmptyDummySession());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private WebSocketSession resetHandler(WebSocketSession session)
    {
        handler = new WebsocketHandlerMcpGatewayRequests();
        try {
            handler.afterConnectionEstablished(session);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        // This injects the real WebsocketHandlerMcpGatewayRequests.sessionsWithUserSessions.UserSession object (salted-key)
        return new DummySession(session.getHandshakeHeaders(),
                Map.of(WSConfig.AUTH_HEADER_KEY, handler.getAllSessions().stream().findAny().get().getValue()));
    }

    /**
     * Set user signed in
     */
    private void authUserId(final WebSocketSession s)
    {
        processorService.processMessage(s,
                new JSONMessage(WebsocketMessageType.REGISTRATION,
                        WSConfig.AUTH_HEADER_KEY + ": " + accessToken,
                        MessageType.USER),
                handler);
    }


    @Test
    public void TestSessionLifeTime() throws InterruptedException {
        resetHandler();
        Assert.isTrue(!handler.hasUserSession(userSessionId), "Session should not exists!");

        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        final WebSocketSession dummySession = getDummySession(); // Session life time started !!!
        Assert.isTrue(handler.isNotExpired(dummySession), "Session should not be expired! Expiry: " + extractExpiry(dummySession));
        Thread.sleep((WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS-1)*1000L);
        // Session should be active t < WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Assert.isTrue(handler.isNotExpired(dummySession), "Session should not be expired! Expiry: " + extractExpiry(dummySession));
        Thread.sleep(1100L);
        // t >= WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Assert.isTrue(handler.isNotExpired(dummySession) == false, "Session should be expired! Expiry: " + extractExpiry(dummySession));
    }

    @Test
    public void TestSessionLifeTimeRefreshExtension() throws InterruptedException {
        resetHandler();
        Assert.isTrue(!handler.hasUserSession(userSessionId), "Session should not exists!");

        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        final WebSocketSession dummySession = getDummySession(); // Session life time started !!!
        Thread.sleep(WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS*1000L);
        // Session should be active t < WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Thread.sleep(100L);
        // t >= WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        // REFRESHING
        handler.refreshSessionExpiry(dummySession, userSessionId);
        Assert.isTrue(handler.isNotExpired(dummySession), "Session should not be expired! Expiry: " + extractExpiry(dummySession));
        Thread.sleep((WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS-1)*1000L);
        // Session should be active t < WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Assert.isTrue(handler.isNotExpired(dummySession), "Session should not be expired! Expiry: " + extractExpiry(dummySession));
        Thread.sleep(1100L);
        // t >= WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Assert.isTrue(handler.isNotExpired(dummySession) == false, "Session should be expired! Expiry: " + extractExpiry(dummySession));

    }

    private String getUserSessionFromSession(final WebSocketSession session)
    {
        final Object o = session.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o!=null && o instanceof UserSession u)
        {
            return String.valueOf(u.getSessionId());
        }
        return null;
    }

    private WebSocketSession getLastSessionFromHandler()
    {
        return handler.getAllSessions().iterator().next().getKey();
    }

    @Test
    public void TestAuthScenarios() throws InterruptedException {
        resetHandler();
        Assert.isTrue(!handler.hasUserSession(userSessionId), "Session should not exists!");
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        WebSocketSession originalSession = getDummySession(); // Session life time started !!!
        // Register session in handler
        WebSocketSession registeredSession = resetHandler(originalSession);
        // Auth dummy session
        authUserId(originalSession);
        handler.isNotExpired(registeredSession); // Mechanism triggers deauthentication
        Assert.isTrue(processorService.isUserSessionAuthenticated(userSessionId), "Expected authenticated after signIn!");
        Thread.sleep((WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS-1)*1000L);
        // Session should be active t < WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        handler.isNotExpired(registeredSession); // Mechanism triggers deauthentication
        Assert.isTrue(processorService.isUserSessionAuthenticated(userSessionId), "Expected authenticated in time!");
        Thread.sleep(1100L);
        // t >= WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        registeredSession = getLastSessionFromHandler();
        handler.isNotExpired(registeredSession); // Mechanism triggers deauthentication
        Assert.isTrue(processorService.isUserSessionAuthenticated(userSessionId) == false, "Expected unauthenticated after session timed out!");
        // Reauth --> REFRESH session
        authUserId(originalSession); // That will also refresh the session
        registeredSession = getLastSessionFromHandler();
        Assert.isTrue(handler.isNotExpired(registeredSession), "Expected MSG 'REGISTER' to refresh the current session!");
        Thread.sleep((WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS-1)*1000L);
        // Session should be active t < WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Assert.isTrue(handler.isNotExpired(registeredSession), "Expected MSG 'REGISTER' to refresh the current session!"); // Mechanism triggers deauthentication
        Assert.isTrue(processorService.isUserSessionAuthenticated(userSessionId), "Expected authenticated  after refresh!");
        Thread.sleep(1100L);
        // t >= WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        registeredSession = getLastSessionFromHandler();
        handler.isNotExpired(registeredSession); // Mechanism triggers deauthentication
        Assert.isTrue(processorService.isUserSessionAuthenticated(userSessionId) == false, "Expected unauthenticated after refresh timed out!");
    }



    private class DummySession extends StandardWebSocketSession implements WebSocketSession, NativeWebSocketSession
    {

        public DummySession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes)
        {
            super(headers, attributes, null, null);
        }

        public DummySession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes, @Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress) {
            super(headers, attributes, localAddress, remoteAddress);
        }

        @Override
        public boolean isOpen() {
            return false; // todo find better way to override abstract session
        }

        @Override
        public void sendTextMessage(TextMessage message) throws IOException {
            System.out.println("Called: sendTextMessage");
        }

        @Override
        protected void sendBinaryMessage(BinaryMessage message) throws IOException {

        }

        @Override
        protected void sendPingMessage(PingMessage message) throws IOException {

        }

        @Override
        protected void sendPongMessage(PongMessage message) throws IOException {

        }

        @Override
        protected void closeInternal(CloseStatus status) throws IOException {

        }
    }

}
