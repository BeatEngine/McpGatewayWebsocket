package org.beatengine.mcpgateway.aiintegration;


import org.beatengine.mcpgateway.McpGatewayApplication;
import org.beatengine.mcpgateway.McpGatewayApplicationTests;
import org.beatengine.mcpgateway.service.ChatBotService;
import org.beatengine.mcpgateway.websocket.JSONMessage;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import java.util.List;

@ContextConfiguration(classes = {McpGatewayApplication.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AIIntegrationTests extends McpGatewayApplicationTests {

    //todo In a real project this test would use a test database also for the chatBotMemory when not in-memory memory

    public AIIntegrationTests()
    {
        //Use localhost for testing, rest from environmentconfigurations.env of root project
        super();
    }

    @Autowired
    AIChatClientService chatClient;

    @Autowired
    ChatBotService chatBotService;

    boolean waited = false;

    private void waitForMcpServerTools()
    {
        if(!waited) {
            waited = true;
            try {
                // Wait for mcp server registration otherwise no tools available
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @Order(1)
    void TestChatBotMemoryFormat()
    {
        waitForMcpServerTools();

        final String chatSession = "tm-328hfh9823jnjwd98jjd892";

        final List<JSONMessage> messagesBefore = chatBotService.queryChatbotMemoryMessages(chatSession);
        Assert.isTrue(messagesBefore.isEmpty(), "There are already messages in this chat! Fix test!");
        // Used incomingChatbotMessage bco. the chat prefix in queryChatbotMemoryMessages!
        final String userInquiry = "Hello! Are you available?";
        chatBotService.incomingChatbotMessage(userInquiry, chatSession);
        final List<JSONMessage> jsonMessages = chatBotService.queryChatbotMemoryMessages(chatSession);

        Assert.isTrue(jsonMessages.size() == 2,
                "Message size out of scope 2 size: " + jsonMessages.size());

        // The last 2 messages should be inquiry and response (mind bots optional 'hello' Message)
        final JSONMessage previous = jsonMessages.get(jsonMessages.size()-2);
        final JSONMessage last = jsonMessages.get(jsonMessages.size()-1);


        Assert.isTrue(MessageType.USER.equals(previous.author), "Expected user author is different! '"
                + MessageType.USER + "' != '" + previous.author + "'");
        Assert.isTrue(MessageType.ASSISTANT.equals(last.author), "Expected system author is different! '"
                + MessageType.ASSISTANT + "' != '" + last.author + "'");

        Assert.isTrue(userInquiry.equals(previous.data), "Expected user inquiry in data '"
                + userInquiry + "' != '" + previous.data + "'");
        Assert.isTrue(!String.valueOf(last.data).isBlank(), "Expected assist response is not blank");

    }

    @Test
    @Order(2)
    void TestOllamaToolCallQuery()
    {
        waitForMcpServerTools();

        final String keyWord = "Battery-234";
        String s = chatClient.promptToOllama("Search for the usage of the product with name " + keyWord + ".",
                "chat23462347642792932758720384828723");
        System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,"Bot: " + s);
        Assert.isTrue(!s.isBlank(), "ChatClient reply is blank.");
        Assert.isTrue(s.contains(keyWord), "Prompt result doesn't match with manual query.");
        // Was completed successfully at my test ollama and alpine ollama qwen2.5:3b
    }

}
