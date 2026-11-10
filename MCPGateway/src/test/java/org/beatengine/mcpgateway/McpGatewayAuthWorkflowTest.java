package org.beatengine.mcpgateway;

import org.beatengine.mcpgateway.websocket.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ContextConfiguration(classes = {McpGatewayApplication.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class McpGatewayAuthWorkflowTest extends McpGatewayApplicationTests {

    @LocalServerPort
    protected int port;

    private String url;

    StandardWebSocketClient client;
    private WebSocketSession session = null;
    private boolean isConnecting = false;

    protected CustomWebSocketHandler handler;

    protected int MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS = 4;

    protected List<String> receivedMessages = new ArrayList<>();

    public McpGatewayAuthWorkflowTest()
    {
        //Load properties
        super();
        System.setProperty("MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS", ""+MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS);
        client = new StandardWebSocketClient();
    }

    protected synchronized void connect() {
        url = "ws://localhost:"+port+"/ws";
        isConnecting = true;
        System.out.println("Connecting...");
        handler = new CustomWebSocketHandler();
        try {
            client.execute(handler, url).get();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            isConnecting = false;
        }
    }

    private void sendMessage(final JSONMessage msg) throws IOException {
        final String m = serializeObject(msg);
        session.sendMessage(new TextMessage(m));
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)+" Client sent: " + m);
    }

/*    Workflow 1
[SYSTEM] REGISTRATION_RESPONSE: auth: false
[SYSTEM] REGISTRATION_RESPONSE: auth: true
[ASSISTANT] AI_RESPONSE: Hello! How can I assist you today?
[SYSTEM] SESSION_EXPIRY: 7303a1f5-01d3-4ddb-8cf1-350f8990d118
[SYSTEM] ERROR: Unauthenticated: {"messageType":"AI_REQUEST","data":"HELLO","author":"USER"}
 */

/* Workflow 2
[SYSTEM] REGISTRATION_RESPONSE: auth: false
[SYSTEM] ERROR: Unauthenticated: {"messageType":"AI_REQUEST","data":"HELLO","author":"USER"}
[SYSTEM] REGISTRATION_RESPONSE: auth: true
[SYSTEM] SESSION_EXPIRY: 7303a1f5-01d3-4ddb-8cf1-350f8990d118
[SYSTEM] ERROR: Unauthenticated: {"messageType":"AI_REQUEST","data":"HELLO","author":"USER"}
*/



@Test
public void TestRegularREGISTRATIONWorkflow1() throws Exception {
    receivedMessages.clear();
    connect();
    Thread.sleep(1000);
    Assert.isTrue(session != null, "WS client connection failed!");

    // 1. False auth
    final JSONMessage messageUnAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
            WSConfig.AUTH_HEADER_KEY + ": " + "#"+MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);
    // 2. Correct auth
    final JSONMessage messageAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
            WSConfig.AUTH_HEADER_KEY + ": " +MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);

    // 3. Check session has expired
    final JSONMessage messageREQ = new JSONMessage(WebsocketMessageType.AI_REQUEST, "Reply with 'Hi I'm an AI!'", MessageType.USER);

    final JSONMessage messageREQ2 = new JSONMessage(WebsocketMessageType.AI_CHAT_HISTORY, "", MessageType.USER);

    int msgc = receivedMessages.size();
    // 1. Fase
    sendMessage(messageUnAuth);
    Thread.sleep(500);
    Assert.isTrue(!receivedMessages.isEmpty(), " No message received");
    msgc = receivedMessages.size();
    final JSONMessage response = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

    Assert.isTrue(response.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

    Object data = response.data;
    if(data instanceof String resData) {
        Assert.isTrue(resData.contains("auth: false"), "Expected: 'auth: false' but:" + resData);
    }
    else
    {
        Assert.isTrue(false, "data isn't instanceof String");
    }

    // 2. Expecting session was closed
    sendMessage(messageREQ);
    Thread.sleep(500);
    Assert.isTrue(receivedMessages.size() > msgc, " No message received");
    msgc = receivedMessages.size();
    final JSONMessage response4 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));
    Assert.isTrue(response4.messageType == WebsocketMessageType.ERROR, "Wrong type not ERROR!");

    // 3. Correct
    sendMessage(messageAuth);
    Thread.sleep(500);
    Assert.isTrue(receivedMessages.size() > msgc, " No message received");
    msgc = receivedMessages.size();
    final JSONMessage response2 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

    Assert.isTrue(response2.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

    Object data2 = response2.data;
    if(data2 instanceof String resData) {
        Assert.isTrue(resData.contains("auth: true"), "Expected: 'auth: true' but:" + resData);
    }
    else
    {
        Assert.isTrue(false, "data isn't instanceof String");
    }

    // 4. Expecting no error
    sendMessage(messageREQ2);
    Thread.sleep(1000);
    Assert.isTrue(receivedMessages.size() == msgc, " There shouldn't be a history in this test");
    final JSONMessage response5 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

    session.close(CloseStatus.NORMAL);
}

    @Test
    public void TestRegularREGISTRATIONWorkflow2() throws Exception {
        receivedMessages.clear();
        connect();
        Thread.sleep(1000);
        Assert.isTrue(session != null, "WS client connection failed!");

        // 1. False auth
        final JSONMessage messageUnAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
                WSConfig.AUTH_HEADER_KEY + ": " + "#"+MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);
        // 2. Correct auth
        final JSONMessage messageAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
                WSConfig.AUTH_HEADER_KEY + ": " +MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);

        // 3. Check session has expired
        final JSONMessage messageREQ = new JSONMessage(WebsocketMessageType.AI_REQUEST, "Reply with 'Hi I'm an AI!'", MessageType.USER);

        int msgc = receivedMessages.size();
        // 1. Fase
        sendMessage(messageUnAuth);
        Thread.sleep(500);
        Assert.isTrue(!receivedMessages.isEmpty(), " No message received");
        msgc = receivedMessages.size();
        final JSONMessage response = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

        Assert.isTrue(response.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

        Object data = response.data;
        if(data instanceof String resData) {
            Assert.isTrue(resData.contains("auth: false"), "Expected: 'auth: false' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }

        // 2. Correct
        sendMessage(messageAuth);
        Thread.sleep(500);
        Assert.isTrue(receivedMessages.size() > msgc, " No message received");
        msgc = receivedMessages.size();
        final JSONMessage response2 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

        Assert.isTrue(response2.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

        Object data2 = response2.data;
        if(data2 instanceof String resData) {
            Assert.isTrue(resData.contains("auth: true"), "Expected: 'auth: true' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }

        // 3. Expecting session was closed
        Thread.sleep(MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS * 1000L);
        sendMessage(messageREQ);
        Thread.sleep(500);
        Assert.isTrue(receivedMessages.size() > msgc, " No message received");
        final JSONMessage responseSessionExpiry = deserializeJSON(receivedMessages.get(receivedMessages.size() - 2));
        final JSONMessage response4 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));
        Assert.isTrue(response4.messageType == WebsocketMessageType.ERROR, "Wrong type not ERROR!");
        Assert.isTrue(responseSessionExpiry.messageType == WebsocketMessageType.SESSION_EXPIRY, "Wrong type not SESSION_EXPIRY!");

        Object data4 = response4.data;
        if(data4 instanceof String resData) {
            Assert.isTrue(resData.contains("Unauthenticated:"), "Expected: 'Unauthenticated:...' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }

        session.close(CloseStatus.NORMAL);
    }

    @Test
    public void TestRegularREGISTRATIONWorkflow3() throws Exception {
        receivedMessages.clear();
        connect();
        Thread.sleep(1000);
        Assert.isTrue(session != null, "WS client connection failed!");

        // 1. Correct auth
        final JSONMessage messageAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
                WSConfig.AUTH_HEADER_KEY + ": " +MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);

        // 2., 3. REQUEST
        final JSONMessage req1 = new JSONMessage(WebsocketMessageType.AI_CHAT_HISTORY, "req1", MessageType.USER);
        final JSONMessage req2 = new JSONMessage(WebsocketMessageType.AI_CHAT_HISTORY, "req1", MessageType.USER);

        int msgc = receivedMessages.size();

        // 1. Auth
        sendMessage(messageAuth);
        Thread.sleep(500);

        Assert.isTrue(receivedMessages.size() > msgc, " No message received");
        msgc = receivedMessages.size();
        final JSONMessage response2 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

        Assert.isTrue(response2.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

        Object data2 = response2.data;
        if(data2 instanceof String resData) {
            Assert.isTrue(resData.contains("auth: true"), "Expected: 'auth: true' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }
        // 2.
        sendMessage(req1);
        Thread.sleep(500);
        Assert.isTrue(receivedMessages.size() == msgc, "Unexpected message received: " + receivedMessages.get(receivedMessages.size()-1));
        // 3.
        sendMessage(req2);
        Thread.sleep(500);
        Assert.isTrue(receivedMessages.size() == msgc, "Unexpected message received: " + receivedMessages.get(receivedMessages.size()-1));


        for(int i = msgc; i < receivedMessages.size(); i++)
        {
            Assert.isTrue(receivedMessages.get(i).contains("Unauthenticated") == false, "Not expected 'Unauthenticated'" + receivedMessages.get(i));
        }
        session.close(CloseStatus.NORMAL);
    }

    @Test
    public void TestRefreshUserSessionByRegistration() throws Exception {
        receivedMessages.clear();

        connect();
        Thread.sleep(1000);
        Assert.isTrue(session != null, "WS client connection failed!");

        final JSONMessage messageAuth = new JSONMessage(WebsocketMessageType.REGISTRATION,
                WSConfig.AUTH_HEADER_KEY + ": " +MCP_GATEWAY_ACCESS_TOKENS, MessageType.USER);

        final JSONMessage req = new JSONMessage(WebsocketMessageType.AI_CHAT_HISTORY, "", MessageType.USER);

        int msgc = receivedMessages.size();

        // 1. Auth
        sendMessage(messageAuth);
        Thread.sleep(500);

        Assert.isTrue(receivedMessages.size() > msgc, " No message received");
        msgc = receivedMessages.size();
        final JSONMessage response2 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

        Assert.isTrue(response2.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

        Object data2 = response2.data;
        if(data2 instanceof String resData) {
            Assert.isTrue(resData.contains("auth: true"), "Expected: 'auth: true' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }
        // 2. Wait till session most expired
        Thread.sleep((MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS-1) * 1000L);
        // 3. Auth again (should be fine)
        sendMessage(messageAuth);
        Thread.sleep(500);

        Assert.isTrue(receivedMessages.size() > msgc, " No message received");
        msgc = receivedMessages.size();
        final JSONMessage response3 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));

        Assert.isTrue(response3.messageType == WebsocketMessageType.REGISTRATION_RESPONSE, "Wrong type not REGISTRATION_RESPONSE!");

        Object data3 = response3.data;
        if(data3 instanceof String resData) {
            Assert.isTrue(resData.contains("auth: true"), "Expected: 'auth: true' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }

        // If refresh has not worked the original session is already expired for 500ms.
        //Thread.sleep(1000);
        // 4. Send a history request (no history in this test)
        sendMessage(req);
        Thread.sleep(500);
        // No further unauthorized messages are there
        Assert.isTrue(receivedMessages.size() == msgc, "Unexpected message received: " + receivedMessages.get(receivedMessages.size()-1));
        msgc = receivedMessages.size();

        for(int i = 0; i < receivedMessages.size(); i++)
        {
            Assert.isTrue(receivedMessages.get(i).contains("Unauthenticated") == false, "Not expected 'Unauthenticated'" + receivedMessages.get(i));
        }

        // 5. Send a history request (no history in this test) after refresh should be timed out.
        // The refresh has worked, but the refreshed session is already expired.
        Thread.sleep(MCP_GATEWAS_WS_SESSION_LIFETIME_SECONDS*1000L);
        System.out.println("Next should return ERROR (not auth)");
        sendMessage(req);
        Thread.sleep(500);
        // No further unauthorized messages are there
        Assert.isTrue(receivedMessages.size() > msgc, "No ERROR message received: " + receivedMessages.get(receivedMessages.size()-1));

        final JSONMessage responseSessionExpired = deserializeJSON(receivedMessages.get(receivedMessages.size() - 2));
        final JSONMessage response4 = deserializeJSON(receivedMessages.get(receivedMessages.size() - 1));
        Assert.isTrue(responseSessionExpired.messageType == WebsocketMessageType.SESSION_EXPIRY, "Wrong type not SESSION_EXPIRY!");
        Assert.isTrue(response4.messageType == WebsocketMessageType.ERROR, "Wrong type not ERROR!");

        if(response4.data instanceof String resData) {
            Assert.isTrue(resData.contains("Unauthenticated:"), "Expected: 'Unauthenticated:' but:" + resData);
        }
        else
        {
            Assert.isTrue(false, "data isn't instanceof String");
        }


        session.close(CloseStatus.NORMAL);
    }

    /**
     * @param obj JSONMessage object
     * @return json String
     */
    protected String serializeObject(final JSONMessage obj)
    {
        final ObjectMapper mapper = JacksonUtils.getDefaultJsonMapper();
        return mapper.writeValueAsString(obj);
    }

    /**
     * @param json json String
     * @return JSONMessage object
     */
    protected JSONMessage deserializeJSON(final String json)
    {
        final ObjectMapper mapper = JacksonUtils.getDefaultJsonMapper();
        return mapper.readValue(json, JSONMessage.class);
    }
    private class CustomWebSocketHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession webSocketSession) {
            session = webSocketSession;
            isConnecting = false;
            System.out.println("WS connected!");
        }

        @Override
        protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) {
            final String payload = message.getPayload();
            receivedMessages.add(payload);
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)+" WS incoming: " + payload);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) {
            System.out.println("WS closed: " + status.getReason());
            isConnecting = false;
            session = null;
        }
    }
}
