package com.tactics.server.ws;

/**
 * Framework-agnostic abstraction of a WebSocket connection.
 */
public interface ClientConnection {

    String getId();

    void sendMessage(String message);
}
