package com.tactics.client.net;

/**
 * Listener interface for WebSocket events.
 * Used by both Desktop and TeaVM implementations.
 */
public interface WebSocketListener {

    /**
     * Called when WebSocket connection is established.
     */
    void onConnected();

    /**
     * Called when a message is received from the server.
     * @param message The raw JSON message string
     */
    void onMessage(String message);

    /**
     * Called when WebSocket connection is closed.
     */
    void onDisconnected();

    /**
     * Called when an error occurs.
     * @param error Description of the error
     */
    void onError(String error);
}
