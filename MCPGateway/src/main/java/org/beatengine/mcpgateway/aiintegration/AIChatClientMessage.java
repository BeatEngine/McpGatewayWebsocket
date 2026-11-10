package org.beatengine.mcpgateway.aiintegration;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;

public class AIChatClientMessage extends AssistantMessage implements Message {
    private String text = "";
    private MessageType type;

    public AIChatClientMessage(final String text, final MessageType type)
    {
        super(text, Map.of("created", DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now())), List.of(), List.of());
        this.text = text;
        this.type = type;
    }

    @Override
    public MessageType getMessageType() {
        return type;
    }

    @Override
    public @Nullable String getText() {
        return text;
    }
}
