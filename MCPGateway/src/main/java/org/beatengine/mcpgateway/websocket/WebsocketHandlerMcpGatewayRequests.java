package org.beatengine.mcpgateway.websocket;

import org.beatengine.mcpgateway.McpGatewayApplication;
import org.beatengine.mcpgateway.service.GatewayMessageProcessorService;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All AI requests from external applications are processed from this Controller-Like Handler
 */
public class WebsocketHandlerMcpGatewayRequests extends AbstractWebSocketHandler {

    private GatewayMessageProcessorService messageProcessorService;

    private final Map<WebSocketSession, UserSession> sessionsWithUserSessions = new ConcurrentHashMap<>();

    private String salt = "WHMGR-";

    private String extractUserSessionId(final UserSession session)
    {
        return session.getSessionId().substring(salt.length());
    }

    private void expire(final WebSocketSession session, final UserSession userSession)
    {
        final String userSessionId = extractUserSessionId(userSession);
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "User expired: " + userSessionId);
        messageProcessorService.onSessionExpired(userSessionId, session, this);
        sessionsWithUserSessions.remove(session);
    }

    private void expireAllExpiredSessions()
    {
        for(Map.Entry<WebSocketSession, UserSession> session : sessionsWithUserSessions.entrySet())
        {
            final UserSession userSession = session.getValue();
            if(userSession.hasExpired())
            {
                expire(session.getKey(), userSession);
            }
        }
    }
    
    public void refreshSessionExpiry(final WebSocketSession session, final String userSessionIdUnsalted)
    {
        Object o = session.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o != null && o instanceof UserSession u)
        {
            String sessionId = u.getSessionId();
            session.getAttributes().put(WSConfig.AUTH_HEADER_KEY, new UserSession(sessionId, WSConfig.WEBSOCKET_SESSION_LIFETIME_SECONDS));
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "User refreshed: " + userSessionIdUnsalted);
        }
        this.registerUserIdBySession(session);
    }

    /**
     * Returns the userSession if found and not expired otherwise remove session on expire
     * @param session
     * @return UserSession.sessionId
     */
    public String getUserIdBySession(final WebSocketSession session)
    {
        final UserSession userIdentifier = sessionsWithUserSessions.get(session);
        if(userIdentifier != null)
        {
            if(!userIdentifier.hasExpired()) {
                return extractUserSessionId(userIdentifier);
            }
            else
            {
                expire(session, userIdentifier);
            }
        }
        return null;
    }
    private final AtomicBoolean expiring = new AtomicBoolean(false);
    private final ExecutorService executorExpiring = Executors.newSingleThreadExecutor();
    /**
     * Generates new session and expires all expired sessions
     * @return new UserSessionId
     */
    public String generateUserSessionId()
    {
        final String usid = UserSession.newId();
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "Generated new user-session: " + usid);
        if (expiring.compareAndSet(false, true)) {
            executorExpiring.submit(() -> {
                try {
                    expireAllExpiredSessions();
                } finally {
                    expiring.set(false);
                }
            });
        }
        return usid;
    }

    private void registerUserIdBySession(final WebSocketSession session)
    {
        final Object o = session.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o!=null && o instanceof UserSession)
        {
            final UserSession userSession = (UserSession)o;
            registerUserIdBySession(session, userSession);
        }
    }

    private void registerUserIdBySession(final WebSocketSession session, final UserSession userId)
    {
        final UserSession scp = userId.copy();
        scp.setSessionId(salt+userId.getSessionId());
        sessionsWithUserSessions.put(session, scp);
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "Registered user: " + scp.getSessionId());
    }

    public boolean hasUserSession(final String userSessionIdUnsalted)
    {
        if(userSessionIdUnsalted == null)
        {
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER,
                    "called hasUserSession(userSessionIdUnsalted=null) (client side cookie expired or new session)");
            return false;
        }
        final UserSession matcher = new UserSession(salt+userSessionIdUnsalted, 0);
        return sessionsWithUserSessions.values().stream().anyMatch(matcher::equalsSessionId);
    }

    /**
     * This method also triggers the expire() --> the event GatewayMessageProcessorService.onSessionExpired
     * @param session ws-session
     * @return true --> If the session is not expired
     */
    public boolean isNotExpired(final WebSocketSession session)
    {
        Object o = session.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o != null && o instanceof UserSession u)
        {
            final boolean hasExpired = u.hasExpired();
            if(hasExpired)
            {
                final UserSession userSession = sessionsWithUserSessions.get(session);
                if(userSession != null) {
                    expire(session, userSession);
                }
            }
            return !hasExpired;
        }
        return true;
    }

    public Set<Map.Entry<WebSocketSession, UserSession>> getAllSessions()
    {
        return sessionsWithUserSessions.entrySet();
    }

    private static WebsocketHandlerMcpGatewayRequests SINGLETON;
    public WebsocketHandlerMcpGatewayRequests()
    {
        SINGLETON = this;
    }

    public static WebsocketHandlerMcpGatewayRequests getInstance()
    {
        return SINGLETON;
    }


    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        if(messageProcessorService == null) {
            // This is my current solution for cycle dependency problems with too many Spring objects
            messageProcessorService = GatewayMessageProcessorService.getInstance();
        }
        registerUserIdBySession(session);
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS connection opened: " + session.getId());

    }

    /**
     * <h6>ENTRY POINT for all textbased websocket messages.</h6>
     * @param session session
     * @param message will be deserialized into JSONMessage type
     * @throws Exception Deserialization exception
     */
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS message text received: " + session.getId());
        final String payload = message.getPayload();
        messageProcessorService.processMessage(session, payload, this);
    }



    /**
     * Send the websocket payload to all user-chats with sessionId (for example this should be changed to userId)
     * @param payload The websocket json-object to send. E.G. JSONMessage
     * @param session UserId has a mapping to session, so multiple tabs would receive the responses
     * @throws IOException Sending over websocket failed
     */
    public void sendTextMessage(final String payload, final WebSocketSession session) throws IOException
    {
        sendTextMessage(payload, session, true);
    }

    /**
     *
     * Send the websocket payload to all sessions with sessionId (for example this should be changed to userId)
     * @param payload The websocket json-object to send. E.G. JSONMessage
     * @param session session
     * @param broadcastToAllCorrespondingUserSessions IF false --> only send to current chat not all related user chats
     * @throws IOException Sending over websocket failed
     */
    public void sendTextMessage(final String payload, final WebSocketSession session,
                                             final boolean broadcastToAllCorrespondingUserSessions) throws IOException
    {

        if(broadcastToAllCorrespondingUserSessions)
        {
            final UserSession userSessionId = this.sessionsWithUserSessions.get(session);
            if(userSessionId != null) {
                for (final Map.Entry<WebSocketSession, UserSession> webSocketSessionWithUser : sessionsWithUserSessions.entrySet()) {
                    // If any websocketSessionId is related to the userSessionId related to the current websocketSessionId
                    if (userSessionId.equalsSessionId(webSocketSessionWithUser.getValue())) {
                        final WebSocketSession webSocketSession = webSocketSessionWithUser.getKey();
                        if (webSocketSession.isOpen()) {
                            webSocketSession.sendMessage(new TextMessage(payload));
                        }
                    }
                }
            }
            else
            {

                Logger.getLogger(this.getClass().getSimpleName()).log(Level.WARNING,
                        "sendTextMessage(): sessionsWithUserSessions.get(session) == null");
            }
        }
        else
        {
            if(session.isOpen())
            {
                session.sendMessage(new TextMessage(payload));
            }
        }

    }

    /**
     * Send the message to all connections
     * @param payload message
     * @throws IOException Sending over websocket failed
     */
    public void broadcastTextMessage(final String payload) throws IOException
    {
        for (final Map.Entry<WebSocketSession, UserSession> webSocketSessionWithUser: sessionsWithUserSessions.entrySet()) {
            final WebSocketSession webSocketSession = webSocketSessionWithUser.getKey();
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    protected void handleBinaryMessage(final WebSocketSession session, final BinaryMessage message) throws Exception {
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.WARNING, "WS message binary received: " + session.getId());

    }

    protected void handlePongMessage(final WebSocketSession session, final PongMessage message) throws Exception {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS pong received: " + session.getId());

    }

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) throws Exception {
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.WARNING, "WS transport failed: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) throws Exception {
        final UserSession removed = sessionsWithUserSessions.remove(session);
        if(sessionsWithUserSessions.values().stream().noneMatch(removed::equalsSessionId))
        {
            // when the user closes the last of his connection, all his sessions expires!
            expire(session, removed);
        }
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS connection closed: " + session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
