package com.tactics.client.net;

import com.badlogic.gdx.Gdx;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.websocket.WebSocket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * TeaVM WebSocket client implementation using browser WebSocket API.
 * Features auto-reconnect with exponential backoff.
 * Uses TeaVM JSO WebSocket API for proper browser integration.
 */
public class TeaVMWebSocketClient implements IWebSocketClient {

    private static final String TAG = "TeaVMWebSocketClient";
    private static final int MAX_RECONNECT_DELAY_MS = 30000;
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000;

    private WebSocket socket;
    private WebSocketListener listener;
    private String serverUrl;
    private boolean autoReconnect = true;
    private boolean intentionalDisconnect = false;
    private int currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
    private boolean connected = false;

    private final Queue<String> outgoingQueue = new LinkedList<>();
    private final List<String> incomingQueue = new ArrayList<>();

    @JSBody(params = {"delay", "callback"}, script = "setTimeout(function() { callback.run(); }, delay);")
    private static native void setTimeout(int delay, Runnable callback);

    @Override
    public void connect(String url) {
        this.serverUrl = url;
        this.intentionalDisconnect = false;
        this.currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
        createAndConnect();
    }

    private void createAndConnect() {
        try {
            log("Connecting to " + serverUrl);
            socket = WebSocket.create(serverUrl);

            socket.onOpen(event -> {
                log("Connected");
                connected = true;
                currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

                // Send queued outgoing messages
                while (!outgoingQueue.isEmpty()) {
                    String msg = outgoingQueue.poll();
                    if (msg != null) {
                        socket.send(msg);
                    }
                }

                if (listener != null) {
                    Gdx.app.postRunnable(() -> listener.onConnected());
                }
            });

            socket.onMessage(event -> {
                String data = event.getData().cast();
                log("Received: " + data);
                // Add to queue for thread-safe polling
                synchronized (incomingQueue) {
                    incomingQueue.add(data);
                }
                // Also notify listener for backwards compatibility
                if (listener != null) {
                    Gdx.app.postRunnable(() -> listener.onMessage(data));
                }
            });

            socket.onClose(event -> {
                log("Disconnected");
                connected = false;

                if (listener != null) {
                    Gdx.app.postRunnable(() -> listener.onDisconnected());
                }

                if (autoReconnect && !intentionalDisconnect) {
                    scheduleReconnect();
                }
            });

            socket.onError(event -> {
                log("Error occurred");
                if (listener != null) {
                    Gdx.app.postRunnable(() -> listener.onError("WebSocket error"));
                }
            });

        } catch (Exception e) {
            log("Failed to create WebSocket: " + e.getMessage());
            if (listener != null) {
                listener.onError("Failed to create connection: " + e.getMessage());
            }
            if (autoReconnect && !intentionalDisconnect) {
                scheduleReconnect();
            }
        }
    }

    private void scheduleReconnect() {
        log("Scheduling reconnect in " + currentReconnectDelay + "ms");

        setTimeout(currentReconnectDelay, () -> {
            if (autoReconnect && !intentionalDisconnect && !connected) {
                log("Attempting reconnect...");
                createAndConnect();
            }
        });

        // Exponential backoff
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
    }

    @Override
    public void send(String message) {
        if (socket != null && connected) {
            log("Sending: " + message);
            socket.send(message);
        } else {
            log("Queuing message (not connected): " + message);
            outgoingQueue.offer(message);
        }
    }

    @Override
    public void disconnect() {
        log("Disconnecting");
        intentionalDisconnect = true;
        connected = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    // WebSocket ready state constants (per WebSocket API spec)
    private static final int WEBSOCKET_OPEN = 1;

    @Override
    public boolean isConnected() {
        return connected && socket != null && socket.getReadyState() == WEBSOCKET_OPEN;
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        this.autoReconnect = enabled;
    }

    @Override
    public String pollMessage() {
        synchronized (incomingQueue) {
            if (incomingQueue.isEmpty()) {
                return null;
            }
            return incomingQueue.remove(0);
        }
    }

    private void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log(TAG, message);
        }
    }
}
