package com.tactics.server.ws;

import org.java_websocket.WebSocket;

/**
 * Adapter from Java-WebSocket's WebSocket to our ClientConnection interface.
 * Wraps the underlying WebSocket connection and provides a stable unique ID.
 */
public class WebSocketClientConnection implements ClientConnection {

    private final String id;
    private final WebSocket webSocket;

    /**
     * Create a new WebSocketClientConnection.
     *
     * @param id        unique identifier for this connection
     * @param webSocket the underlying WebSocket connection
     */
    public WebSocketClientConnection(String id, WebSocket webSocket) {
        this.id = id;
        this.webSocket = webSocket;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void sendMessage(String message) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(message);
        }
    }

    /**
     * Get the underlying WebSocket connection.
     *
     * @return the wrapped WebSocket
     */
    public WebSocket getWebSocket() {
        return webSocket;
    }
}
