package com.tactics.server.dto;

/**
 * Generic wrapper for outgoing WS messages.
 */
public class OutgoingMessage {

    private final String type;
    private final Object payload;

    public OutgoingMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}
