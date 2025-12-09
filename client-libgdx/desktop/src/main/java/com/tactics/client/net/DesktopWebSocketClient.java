package com.tactics.client.net;

import com.badlogic.gdx.Gdx;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Desktop WebSocket client implementation using java-websocket library.
 * Features auto-reconnect with exponential backoff.
 */
public class DesktopWebSocketClient implements IWebSocketClient {

    private static final String TAG = "DesktopWebSocketClient";
    private static final int MAX_RECONNECT_DELAY_MS = 30000; // 30 seconds max
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000; // 1 second initial

    private WebSocketClient client;
    private WebSocketListener listener;
    private String serverUrl;
    private boolean autoReconnect = true;
    private boolean intentionalDisconnect = false;
    private int currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

    private final Queue<String> outgoingQueue = new LinkedList<>();
    private final ConcurrentLinkedQueue<String> incomingQueue = new ConcurrentLinkedQueue<>();
    private boolean connected = false;

    @Override
    public void connect(String url) {
        this.serverUrl = url;
        this.intentionalDisconnect = false;
        this.currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
        createAndConnect();
    }

    private void createAndConnect() {
        try {
            URI uri = new URI(serverUrl);
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    Gdx.app.log(TAG, "Connected to " + serverUrl);
                    connected = true;
                    currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

                    // Send queued outgoing messages
                    while (!outgoingQueue.isEmpty()) {
                        String msg = outgoingQueue.poll();
                        if (msg != null) {
                            send(msg);
                        }
                    }

                    if (listener != null) {
                        Gdx.app.postRunnable(() -> listener.onConnected());
                    }
                }

                @Override
                public void onMessage(String message) {
                    Gdx.app.log(TAG, "Received: " + message);
                    // Add to queue for thread-safe polling
                    incomingQueue.add(message);
                    // Also notify listener for backwards compatibility
                    if (listener != null) {
                        Gdx.app.postRunnable(() -> listener.onMessage(message));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Gdx.app.log(TAG, "Disconnected: " + reason + " (code: " + code + ")");
                    connected = false;

                    if (listener != null) {
                        Gdx.app.postRunnable(() -> listener.onDisconnected());
                    }

                    if (autoReconnect && !intentionalDisconnect) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Gdx.app.error(TAG, "WebSocket error: " + ex.getMessage());
                    if (listener != null) {
                        Gdx.app.postRunnable(() -> listener.onError(ex.getMessage()));
                    }
                }
            };

            Gdx.app.log(TAG, "Connecting to " + serverUrl);
            client.connect();

        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to create WebSocket: " + e.getMessage());
            if (listener != null) {
                listener.onError("Failed to create connection: " + e.getMessage());
            }
            if (autoReconnect && !intentionalDisconnect) {
                scheduleReconnect();
            }
        }
    }

    private void scheduleReconnect() {
        Gdx.app.log(TAG, "Scheduling reconnect in " + currentReconnectDelay + "ms");

        new Thread(() -> {
            try {
                Thread.sleep(currentReconnectDelay);
                if (autoReconnect && !intentionalDisconnect && !connected) {
                    Gdx.app.log(TAG, "Attempting reconnect...");
                    createAndConnect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Exponential backoff: double the delay, up to max
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
    }

    @Override
    public void send(String message) {
        if (client != null && connected) {
            Gdx.app.log(TAG, "Sending: " + message);
            client.send(message);
        } else {
            // Queue message if not connected
            Gdx.app.log(TAG, "Queuing message (not connected): " + message);
            outgoingQueue.offer(message);
        }
    }

    @Override
    public void disconnect() {
        Gdx.app.log(TAG, "Disconnecting");
        intentionalDisconnect = true;
        connected = false;
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isConnected() {
        return connected && client != null && client.isOpen();
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        this.autoReconnect = enabled;
    }

    @Override
    public String pollMessage() {
        return incomingQueue.poll();
    }
}
