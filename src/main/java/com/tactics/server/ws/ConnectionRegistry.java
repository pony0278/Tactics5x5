package com.tactics.server.ws;

import java.util.Map;

/**
 * Tracks active connections and their mapping to matches/slots.
 */
public class ConnectionRegistry {

    private final Map<String, ClientConnection> connections;

    public ConnectionRegistry(Map<String, ClientConnection> connections) {
        this.connections = connections;
    }

    public Map<String, ClientConnection> getConnections() {
        return connections;
    }

    public void register(ClientConnection connection) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void unregister(ClientConnection connection) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ClientConnection findById(String id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
