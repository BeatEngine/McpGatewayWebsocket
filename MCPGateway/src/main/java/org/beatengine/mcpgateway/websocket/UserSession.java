package org.beatengine.mcpgateway.websocket;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserSession {

    private String sessionId;
    private final LocalDateTime expiry;

    public UserSession(final String sessionId, final int expirySeconds)
    {
        this.sessionId = sessionId;
        this.expiry = LocalDateTime.now().plusSeconds(expirySeconds);
    }

    private UserSession(final String sessionId, final LocalDateTime expiry)
    {
        this.sessionId = sessionId;
        this.expiry = expiry;
    }

    public UserSession copy()
    {
        return new UserSession(this.sessionId, this.expiry);
    }

    public boolean hasExpired() {
        return !LocalDateTime.now().isBefore(expiry);
    }

    public String getSessionId() {
        return sessionId;
    }

    protected void setSessionId(final String sessionId)
    {
        this.sessionId = sessionId;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public boolean equalsSessionId(final Object o)
    {
        if(o instanceof UserSession u)
        {
            return this.getSessionId().equals(u.getSessionId());
        }
        return false;
    }

    public static String newId()
    {
        return UUID.randomUUID().toString();
    }
}
