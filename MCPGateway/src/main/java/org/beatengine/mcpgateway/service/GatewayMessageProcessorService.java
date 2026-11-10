package org.beatengine.mcpgateway.service;

import io.github.springwolf.core.asyncapi.annotations.*;
import org.beatengine.mcpgateway.McpGatewayApplication;
import org.beatengine.mcpgateway.websocket.*;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class GatewayMessageProcessorService {
    private static GatewayMessageProcessorService SINGLETON;
    private MessageDigest digest = null;

    private final Set<String> accessTokens = new HashSet<>();

    // Here are all user sessions stored
    private final Map<String, Boolean> authenticatedUserSessionIds = new ConcurrentHashMap<>();

    private String getUserSessionFromSession(final WebSocketSession session)
    {
        final Object o = session.getAttributes().get(WSConfig.AUTH_HEADER_KEY);
        if(o!=null && o instanceof UserSession u)
        {
            return String.valueOf(u.getSessionId());
        }
        return null;
    }

    /**
     * Event when the session has expired.
     * @param session session
     */
    public void onSessionExpired(final String userSessionId,final WebSocketSession session, final WebsocketHandlerMcpGatewayRequests handler)
    {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "UserSession: " + userSessionId + " s: " + getUserSessionFromSession(session));
        authenticatedUserSessionIds.remove(userSessionId);
        final JSONMessage message = new JSONMessage(WebsocketMessageType.SESSION_EXPIRY, userSessionId, MessageType.SYSTEM);
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "onSessionExpired: " + session.getId()
                + " remaining: " + authenticatedUserSessionIds.size());
        try {
            handler.sendTextMessage(serializeObject(message), session, false
                    /* false --> otherwise all sessions would try to re auth at the same time*/);
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE, "Failed send SESSION_EXPIRY: " + e.getMessage());
        }
    }

    /**
     * @param userSessionIdUnsalted getUserSessionFromSession
     * @return If the user-session (from cookie) was authenticated by a valid access-token (WebsocketMessageType.REGISTRATION)
     */
    public boolean isUserSessionAuthenticated(final String userSessionIdUnsalted)
    {
        Boolean b = authenticatedUserSessionIds.get(userSessionIdUnsalted);
        if(b == null)
        {
            return false;
        }
        return b;
    }

    /**
     * @param trail Access-Token
     * @param userSessionId getUserSessionFromSession
     * @return true if successfully authenticated otherwise it deauthenticate the session
     */
    private boolean authenticateUserSession(final String trail, final String userSessionId,final WebSocketSession session , final WebsocketHandlerMcpGatewayRequests handler)
    {
        boolean auth = accessTokens.stream().anyMatch(x -> x.contains(trail));
        authenticatedUserSessionIds.put(userSessionId, auth);
        if(auth)
        {
            // For the case the session would be expired a REGISTRATION always refresh the session
            handler.refreshSessionExpiry(session, userSessionId);
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE,
                    "Authenticated user: " + userSessionId);
        }
        else
        {
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE,
                    "Deauthenticated user: " + userSessionId);
        }

        return auth;
    }

    private void initAccessTokens()
    {
        final String mcpGatewayAccessTokens = System.getProperty("MCP_GATEWAY_ACCESS_TOKENS");
        if(mcpGatewayAccessTokens.isBlank())
        {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.WARNING,
                    "No access-tokens (MCP_GATEWAY_ACCESS_TOKENS=abc,def) set! ");
        }
        if(mcpGatewayAccessTokens.contains(","))
        {
            accessTokens.addAll(Arrays.stream(mcpGatewayAccessTokens.split(",\\s*")).toList());
        }
        else
        {
            accessTokens.add(mcpGatewayAccessTokens);
        }
    }

    public GatewayMessageProcessorService() throws NoSuchAlgorithmException {
        SINGLETON = this;
        digest = MessageDigest.getInstance("SHA-256");
        initAccessTokens();
    }

    @Autowired
    ChatBotService chatBotService;

    public static GatewayMessageProcessorService getInstance() {
        return SINGLETON;
    }

    private static int envMaxThreads()
    {
        final String maxThreads = System.getenv("MCP_GATEWAY_MAX_THREADS");
        int tc = 8;
        try {
            tc = Integer.valueOf(maxThreads);
            System.getLogger(WebsocketHandlerMcpGatewayRequests.class.getSimpleName()).
                    log(System.Logger.Level.INFO, "MCP_GATEWAY_MAX_THREADS="+tc);
        }catch (NumberFormatException nfe)
        {
            System.getLogger(WebsocketHandlerMcpGatewayRequests.class.getSimpleName()).
                    log(System.Logger.Level.INFO, "MCP_GATEWAY_MAX_THREADS= default:"+tc);
        }
        return tc;
    }

    final private ExecutorService executorServiceChat = Executors.newFixedThreadPool(envMaxThreads());

    @AsyncPublisher(operation = @AsyncOperation( //ONLY FOR Springwolf API-DOC
            channelName = "ws",
            description = "Central message output",
            payloadType = JSONMessage.class/*,
            headers = @AsyncOperation.Headers(
                    schemaName = "WebsocketMessageHeaders",
                    values = {
                            @AsyncOperation.Headers.Header(
                                    name = WSConfig.AUTH_HEADER_KEY,
                                    description = "Bearer Token auth",
                                    value = "Bearer xxx"
                            )
                    }
            )*/
    ))
    public void _testSpringwolf(final JSONMessage payload) throws Exception {
        //DUMMY METHOD ONLY FOR API DOC
        throw new Exception("Never call this method!");
    }

    public void processMessage(final WebSocketSession session, final String message,
                               final WebsocketHandlerMcpGatewayRequests handler)
    {
        try {
            processMessage(session, deserializeJSON(message), handler);
        } catch (InvalidFormatException e)
        {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR,
                    "WS deserialization failed " + session.getId() + " : " + e.getMessage());
            handleSerializationErrorRequest(session, message, "", handler);
        }
        catch (Exception ex)
        {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR,
                    "WS deserialization failed " + session.getId() + " : " + ex.getMessage());
            handleSerializationErrorRequest(session, message, ex.getMessage(), handler);
        }
    }


    @AsyncListener(operation = @AsyncOperation( //ONLY FOR Springwolf API-DOC
            channelName = "ws",
            description = "Central entry point JSONMessage",
            payloadType = JSONMessage.class/*,
            headers = @AsyncOperation.Headers(
                    schemaName = "WebsocketMessageHeaders",
                    values = {
                            @AsyncOperation.Headers.Header(
                                    name = WSConfig.AUTH_HEADER_KEY,
                                    description = "Bearer Token auth",
                                    value = "Bearer xxx"
                            )
                    }
            )*/
    ))
    public void processMessage(final WebSocketSession session, final JSONMessage message,
                               final WebsocketHandlerMcpGatewayRequests handler)
    {

        if(message.messageType == WebsocketMessageType.REGISTRATION) {
            handleUserRegistration(session, message, handler);
        }
        else if(handler.isNotExpired(session) && isUserSessionAuthenticated(getUserSessionFromSession(session))) {
            //IF WSConfig.AUTH_HEADER_KEY is VALID --> IF Authenticated
            if (message.messageType == WebsocketMessageType.AI_REQUEST) {
                handleAIRequest(session, message, handler);
            } else if (message.messageType == WebsocketMessageType.AI_CHAT_HISTORY) {
                handleAIChatHistory(session, message, handler);
            }
        }
        else
        {
            handleUnauthenticatedRequest(session, message, handler);
        }

    }

    private void handleUserRegistration(final WebSocketSession session, final JSONMessage message,
                                     final WebsocketHandlerMcpGatewayRequests handler) {


        if(message.data instanceof String) {

                //Registration
                final String authenticationAttempt = String.valueOf(message.data).trim();

                if(authenticationAttempt.startsWith(WSConfig.AUTH_HEADER_KEY+":"))
                {
                    final String trailToken = authenticationAttempt.
                            substring(WSConfig.AUTH_HEADER_KEY.length()+1).trim();
                    //Authentication (by token)
                    final boolean auth = authenticateUserSession(trailToken, getUserSessionFromSession(session), session, handler);
                    sendUserRegistrationResponse(session, new JSONMessage(WebsocketMessageType.REGISTRATION_RESPONSE,
                                    "auth: " + auth,
                                    MessageType.SYSTEM)
                            , handler);
                }
                else
                {
                    sendUserRegistrationResponse(session, new JSONMessage(WebsocketMessageType.REGISTRATION_RESPONSE,
                                    "error: data is not " + WSConfig.AUTH_HEADER_KEY,
                                    MessageType.SYSTEM)
                            , handler);
                }

        }
        else
        {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.WARNING, "WS REGISTRATION wrong format!");
            sendUserRegistrationResponse(session, new JSONMessage(WebsocketMessageType.REGISTRATION_RESPONSE,
                    "error: data wrong format!",
                    MessageType.SYSTEM)
            , handler);
        }
    }

    private void sendUserRegistrationResponse(final WebSocketSession session, final JSONMessage responseMessage,
                                        final WebsocketHandlerMcpGatewayRequests handler)
    {
        try {
            handler.sendTextMessage(serializeObject(responseMessage), session, false);
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.WARNING, e.getMessage());
        }
    }

    /**
     * Handles the message for inquiry
     * @param session ses
     * @param message msg
     */
    private void handleAIRequest(final WebSocketSession session, final JSONMessage message,
                                 final WebsocketHandlerMcpGatewayRequests handler)
    {
        // For the case of incoming data from chatBot UI

            executorServiceChat.submit(new Runnable() {
                // Reply with a websocket message chatbot response in a new thread.
                @Override
                public void run() {
                    final String sessionId = session.getId();
                    final String userSessionId = handler.getUserIdBySession(session);
                    if(userSessionId != null) {

                        /* Passing the inquiry from the message to the LLM over ChatBotService  */
                        final String promptResponse = chatBotService.incomingChatbotMessage(String.valueOf(message.data), userSessionId);
                        try {
                            final String payload = serializeObject(new JSONMessage(WebsocketMessageType.AI_RESPONSE, promptResponse, MessageType.ASSISTANT));
                            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "handleAIRequest: send response");
                            //Send to all related userSessionIds not only to session!
                            handler.sendTextMessage(payload, session, false);
                        } catch (Exception e) {
                            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR, "WS chatbot response failed " + sessionId + " : " + e.getMessage());
                        }
                    }
                    else
                    {
                        // E.g. session has expired
                        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "handleAIRequest: userSessionId == null: " + sessionId);
                        handleUnauthenticatedRequest(session, message , handler);
                    }
                }
            });
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS AI_REQUEST: " + session.getId());
    }

    /**
     * Handles the message that request to fetch the chat history
     * @param session ses IMPORTANT FOR this we only respond to the current chat not all user chats.
     * @param message msg
     */
    private void handleAIChatHistory(final WebSocketSession session, final JSONMessage message,
                                     final WebsocketHandlerMcpGatewayRequests handler)
    {
        if(message.messageType == WebsocketMessageType.AI_CHAT_HISTORY)
        {
            // For the case of incoming data from chatBot UI

            executorServiceChat.submit(new Runnable() {
                // Reply with a websocket message chatbot response in a new thread.
                @Override
                public void run() {
                    final String sessionId = session.getId();
                    final String userSessionId = handler.getUserIdBySession(session);

                    /* Fetching the chatbots memory aka. chat history messages for the first site load  */
                    final List<JSONMessage> historyMessages = chatBotService.queryChatbotMemoryMessages(userSessionId);
                    Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "handleAIChatHistory messages: " + historyMessages.size());
                    for(final JSONMessage jsonMessage : historyMessages) {
                        try {
                            final String payload = serializeObject(jsonMessage);
                            //ONLY SEND to current chat not every user chat
                            handler.sendTextMessage(payload, session, false);
                        } catch (Exception e) {
                            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR, "WS chatbot response failed " + sessionId + " : " + e.getMessage());
                        }
                    }
                }
            });
            Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE, "WS message chatbot received: " + session.getId());
        }
    }

    /**
     * Handles the message for unauthenticated requests
     * @param session ses IMPORTANT FOR this we only respond to the current chat not all user chats.
     * @param message msg
     */
    private void handleUnauthenticatedRequest(final WebSocketSession session, final JSONMessage message,
                                              final WebsocketHandlerMcpGatewayRequests handler)
    {
        try {
            final String payload = serializeObject(new JSONMessage(WebsocketMessageType.ERROR,"Unauthenticated: " + serializeObject(message), MessageType.SYSTEM));
            //ONLY SEND to current connection that it is not authenticated
            handler.sendTextMessage(payload, session, false);
        } catch (Exception e) {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR, "WS chatbot response failed " + session.getId() + " : " + e.getMessage());
        }
    }

    /**
     * Handles the message for unauthenticated requests
     * @param session ses IMPORTANT FOR this we only respond to the current chat not all user chats.
     * @param payload received wrong-formatted msg
     */
    private void handleSerializationErrorRequest(final WebSocketSession session, final String payload, final String error,
                                              final WebsocketHandlerMcpGatewayRequests handler)
    {
        try {
            final String msg = serializeObject(new JSONMessage(WebsocketMessageType.ERROR,"Deserialization Failed ("+error+"): " + payload, MessageType.SYSTEM));
            //ONLY SEND to current connection that the message was not able to be serialized.
            handler.sendTextMessage(msg, session, false);
        } catch (Exception e) {
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.ERROR, "WS chatbot response failed " + session.getId() + " : " + e.getMessage());
        }
    }

    /**
     * @param obj JSONMessage object
     * @return json String
     */
    private String serializeObject(final JSONMessage obj)
    {
        final ObjectMapper mapper = JacksonUtils.getDefaultJsonMapper();
        return mapper.writeValueAsString(obj);
    }

    /**
     * @param json json String
     * @return JSONMessage object
     */
    private JSONMessage deserializeJSON(final String json)
    {
        final ObjectMapper mapper = JacksonUtils.getDefaultJsonMapper();
        return mapper.readValue(json, JSONMessage.class);
    }

}
