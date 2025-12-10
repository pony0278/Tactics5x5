package com.tactics.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.net.WebSocketListener;

import java.util.LinkedList;
import java.util.Queue;

/**
 * GWT-specific WebSocket implementation.
 * Uses JSNI (JavaScript Native Interface) to access browser WebSocket API.
 */
public class GwtWebSocketClient implements IWebSocketClient {

    private JavaScriptObject socket;
    private WebSocketListener listener;
    private boolean connected = false;
    private boolean autoReconnect = false;
    private String lastUrl;

    // Message queue for thread-safe polling (GWT is single-threaded, but keeps API consistent)
    private final Queue<String> messageQueue = new LinkedList<>();

    @Override
    public void connect(String url) {
        this.lastUrl = url;
        createWebSocket(url);
    }

    @Override
    public void send(String message) {
        if (connected && socket != null) {
            sendNative(socket, message);
        }
    }

    @Override
    public void disconnect() {
        autoReconnect = false;  // Disable auto-reconnect on manual disconnect
        if (socket != null) {
            closeNative(socket);
            socket = null;
            connected = false;
        }
    }

    @Override
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        this.autoReconnect = enabled;
    }

    @Override
    public String pollMessage() {
        return messageQueue.poll();
    }

    // Native JavaScript methods (JSNI)

    private native void createWebSocket(String url) /*-{
        var self = this;
        var ws = new WebSocket(url);

        ws.onopen = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onOpen()();
        };

        ws.onmessage = function(event) {
            var data = event.data;
            self.@com.tactics.client.gwt.GwtWebSocketClient::onMessage(Ljava/lang/String;)(data);
        };

        ws.onclose = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onClose()();
        };

        ws.onerror = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onError(Ljava/lang/String;)("WebSocket error");
        };

        this.@com.tactics.client.gwt.GwtWebSocketClient::socket = ws;
    }-*/;

    private native void sendNative(JavaScriptObject socket, String message) /*-{
        if (socket && socket.readyState === 1) {
            socket.send(message);
        }
    }-*/;

    private native void closeNative(JavaScriptObject socket) /*-{
        if (socket) {
            socket.close();
        }
    }-*/;

    // Callbacks from JavaScript

    private void onOpen() {
        connected = true;
        if (listener != null) {
            listener.onConnected();
        }
    }

    private void onMessage(String data) {
        if (data != null) {
            // Add to queue for thread-safe polling
            messageQueue.add(data);
            // Also notify listener for immediate handling if needed
            if (listener != null) {
                listener.onMessage(data);
            }
        }
    }

    private void onClose() {
        connected = false;
        if (listener != null) {
            listener.onDisconnected();
        }
        // Auto-reconnect logic
        if (autoReconnect && lastUrl != null) {
            scheduleReconnect();
        }
    }

    private void onError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    private native void scheduleReconnect() /*-{
        var self = this;
        $wnd.setTimeout(function() {
            var url = self.@com.tactics.client.gwt.GwtWebSocketClient::lastUrl;
            if (url) {
                self.@com.tactics.client.gwt.GwtWebSocketClient::connect(Ljava/lang/String;)(url);
            }
        }, 3000);  // Reconnect after 3 seconds
    }-*/;
}
