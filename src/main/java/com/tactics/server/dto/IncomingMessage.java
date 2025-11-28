package com.tactics.server.dto;

import java.util.Map;

/**
 * Generic wrapper for incoming WS messages.
 */
public class IncomingMessage {

    private final String type;
    private final Map<String, Object> payload;

    public IncomingMessage(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
