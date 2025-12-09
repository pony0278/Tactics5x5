package com.tactics.client.net;

/**
 * Platform-independent WebSocket client interface.
 * Implementations: DesktopWebSocketClient, TeaVMWebSocketClient
 */
public interface IWebSocketClient {

    /**
     * Connect to a WebSocket server.
     * @param url The WebSocket URL (e.g., "ws://localhost:8080/match")
     */
    void connect(String url);

    /**
     * Send a message to the server.
     * @param message The JSON message string to send
     */
    void send(String message);

    /**
     * Disconnect from the server.
     */
    void disconnect();

    /**
     * Set the listener for WebSocket events.
     * @param listener The listener to receive events
     */
    void setListener(WebSocketListener listener);

    /**
     * Check if currently connected.
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Enable or disable auto-reconnect.
     * @param enabled true to enable auto-reconnect
     */
    void setAutoReconnect(boolean enabled);

    /**
     * Poll a message from the queue.
     * This is the thread-safe way to retrieve messages for rendering.
     * Call this in your render/update loop instead of relying on callbacks.
     * @return The next message in the queue, or null if empty
     */
    String pollMessage();
}
