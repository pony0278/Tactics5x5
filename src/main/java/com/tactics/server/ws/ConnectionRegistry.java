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
        connections.put(connection.getId(), connection);
    }

    public void unregister(ClientConnection connection) {
        connections.remove(connection.getId());
    }

    public ClientConnection findById(String id) {
        return connections.get(id);
    }
}
