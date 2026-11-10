package org.beatengine.mcpgateway.websocket;

public enum WebsocketMessageType {
    REGISTRATION("REGISTRATION"),
    REGISTRATION_RESPONSE("REGISTRATION_RESPONSE"),
    ERROR("ERROR"),
    SESSION_EXPIRY("SESSION_EXPIRY"),
    AI_CHAT_HISTORY("AI_CHAT_HISTORY"),
    AI_REQUEST("AI_REQUEST"),
    AI_RESPONSE("AI_RESPONSE");

    private final String id;

    private WebsocketMessageType(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
