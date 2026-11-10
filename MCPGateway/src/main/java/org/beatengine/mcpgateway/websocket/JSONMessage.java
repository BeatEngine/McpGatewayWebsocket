package org.beatengine.mcpgateway.websocket;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.chat.messages.MessageType;


import java.util.Objects;

public class JSONMessage {
    @Schema(
            description = "WebsocketMessageType",
            example = "REGISTRATION",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public WebsocketMessageType messageType;
    @Schema(
            description = "MessageType",
            example = "USER",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public MessageType author = null;
    @Schema(
            description = "Any data",
            example = (WSConfig.AUTH_HEADER_KEY+": 1234-1234-1234..."),
            requiredMode = Schema.RequiredMode.AUTO
    )
    public Object data = null;

    public JSONMessage()
    {

    }

    public JSONMessage(final WebsocketMessageType messageType, final Object data, final MessageType author) {
        this.messageType = messageType;
        this.author = author;
        this.data = data;
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof JSONMessage)
        {
            final JSONMessage other = (JSONMessage) obj;
            return Objects.equals(this.messageType, other.messageType)
                    && Objects.equals(this.author, other.author)
                    && Objects.equals(this.data, other.data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, author, data);
    }
}
