package com.tactics.server;

import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.MatchRegistry;
import com.tactics.server.core.MatchService;
import com.tactics.server.ws.ClientConnection;
import com.tactics.server.ws.ConnectionRegistry;
import com.tactics.server.ws.MatchWebSocketHandler;
import com.tactics.server.ws.WebSocketClientConnection;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone WebSocket server entrypoint for 5x5 Tactics.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Port: 8080</li>
 *   <li>WebSocket path: /ws (ws://localhost:8080/ws)</li>
 * </ul>
 *
 * <p>How to run from IntelliJ IDEA:
 * <ol>
 *   <li>Create a new Application run configuration</li>
 *   <li>Set Main class to: com.tactics.server.ServerMain</li>
 *   <li>Run the configuration</li>
 *   <li>Connect browser to: ws://localhost:8080/ws</li>
 * </ol>
 */
public class ServerMain {

    private static final int PORT = 8080;
    private static final String WS_PATH = "/ws";

    public static void main(String[] args) {
        // Initialize server components
        MatchRegistry matchRegistry = new MatchRegistry(new ConcurrentHashMap<>());
        RuleEngine ruleEngine = new RuleEngine();
        GameStateSerializer gameStateSerializer = new GameStateSerializer();
        MatchService matchService = new MatchService(matchRegistry, ruleEngine, gameStateSerializer);

        ConnectionRegistry connectionRegistry = new ConnectionRegistry(new ConcurrentHashMap<>());
        MatchWebSocketHandler matchWebSocketHandler = new MatchWebSocketHandler(matchService, connectionRegistry);

        // Start WebSocket server
        TacticsWebSocketServer server = new TacticsWebSocketServer(PORT, matchWebSocketHandler);
        server.start();

        System.out.println("5x5 Tactics WebSocket server running on port " + PORT);
        System.out.println("Connect to: ws://localhost:" + PORT + WS_PATH);
    }

    /**
     * WebSocket server implementation that delegates all events to MatchWebSocketHandler.
     */
    private static class TacticsWebSocketServer extends WebSocketServer {

        private final MatchWebSocketHandler handler;
        private final Map<WebSocket, WebSocketClientConnection> connectionMap;

        public TacticsWebSocketServer(int port, MatchWebSocketHandler handler) {
            super(new InetSocketAddress(port));
            this.handler = handler;
            this.connectionMap = new ConcurrentHashMap<>();
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            String resourcePath = handshake.getResourceDescriptor();

            // Only accept connections on the /ws path
            if (!WS_PATH.equals(resourcePath)) {
                conn.close(1003, "Invalid path. Use " + WS_PATH);
                return;
            }

            // Create adapter and store mapping
            String id = UUID.randomUUID().toString();
            WebSocketClientConnection clientConnection = new WebSocketClientConnection(id, conn);
            connectionMap.put(conn, clientConnection);

            // Delegate to handler
            handler.onOpen(clientConnection);
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            WebSocketClientConnection clientConnection = connectionMap.remove(conn);
            if (clientConnection != null) {
                handler.onClose(clientConnection);
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            WebSocketClientConnection clientConnection = connectionMap.get(conn);
            if (clientConnection != null) {
                handler.onMessage(clientConnection, message);
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("WebSocket error: " + ex.getMessage());
            if (conn != null) {
                WebSocketClientConnection clientConnection = connectionMap.remove(conn);
                if (clientConnection != null) {
                    handler.onClose(clientConnection);
                }
            }
        }

        @Override
        public void onStart() {
            System.out.println("WebSocket server started successfully.");
        }
    }
}
