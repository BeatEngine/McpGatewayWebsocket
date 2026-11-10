package org.beatengine.mcpgateway.service;


import org.beatengine.mcpgateway.McpGatewayApplication;
import org.beatengine.mcpgateway.aiintegration.AIChatClientService;
import org.beatengine.mcpgateway.websocket.JSONMessage;
import org.beatengine.mcpgateway.websocket.WebsocketMessageType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatBotService {
    private static ChatBotService SINGLETON;
    public ChatBotService()
    {
        SINGLETON = this;
    }

    @Autowired
    private AIChatClientService chatClientService;

    private static String ChatConversationPrefix = "gateway-";

    public static ChatBotService getInstance() {
        return SINGLETON;
    }

    /**
     * <h6>Retrieve LLM response for the inquiry.</h6>
     * @param message inquiry for the LLM.
     * @param chatSession The chatSession of the Chat. //todo in a real project this is the userSessionId
     * @return The LLM reply.
     */
    public String incomingChatbotMessage(final String message, final String chatSession)
    {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER, "chat-session: " + chatSession);
        final String s;
        final Instant before = Instant.now();
        s = chatClientService.promptToOllama(message, ChatConversationPrefix + chatSession);
        final Instant after = Instant.now();
        long diffMilli = Duration.between(before, after).toMillis();
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.INFO,"Prompt time: " + diffMilli + " ms");
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER,
                s.substring(0, Math.min(50, s.length())));
        return s;
    }

    /**
     * Query all history messages form the chatbots 'Memory'
     * @param chatSession the chatSession
     * @return the List of JSONMessages from history (memory).
     */
    public List<JSONMessage> queryChatbotMemoryMessages(final String chatSession)
    {
        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE,"queryChatbotMemoryMessages: ");
        final List<JSONMessage> transportMessages = new ArrayList<>();
        final List<Message> messages = chatClientService.queryChatbotMemoryMessages(ChatConversationPrefix+chatSession);
        for(final Message m : messages)
        {
            if(m instanceof AssistantMessage assistantMessage) {
                final MessageType author = m.getMessageType();
                final String data = m.getText();
                // Filter secondary empty tool call steps
                if(!data.isEmpty() || !assistantMessage.hasToolCalls()) {
                    //todo final LocalDateTime created = (LocalDateTime) message.getMetadata().get("created");
                    transportMessages.add(new JSONMessage(WebsocketMessageType.AI_RESPONSE, data, author));
                }
            }
            else if(m instanceof UserMessage)
            {
                final MessageType author = m.getMessageType();
                final String data = m.getText();
                transportMessages.add(new JSONMessage(WebsocketMessageType.AI_RESPONSE, data, author));
            }
        }
        return transportMessages;
    }

}