package com.tactics.server;

import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.MatchRegistry;
import com.tactics.server.core.MatchService;
import com.tactics.server.ws.ConnectionRegistry;
import com.tactics.server.ws.MatchWebSocketHandler;
import com.tactics.server.ws.TacticsWebSocketEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Tactics 5x5 server.
 *
 * Usage:
 *   mvn compile exec:java
 *   or
 *   java -jar target/tactics-engine-1.0-SNAPSHOT.jar
 *
 * Server will start on port 8080 by default.
 * - WebSocket endpoint: ws://localhost:8080/ws
 * - Static files (client): http://localhost:8080/
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = getPort(args);

        // Initialize game services
        MatchRegistry matchRegistry = new MatchRegistry();
        RuleEngine ruleEngine = new RuleEngine();
        GameStateSerializer serializer = new GameStateSerializer();
        MatchService matchService = new MatchService(matchRegistry, ruleEngine, serializer);
        ConnectionRegistry connectionRegistry = new ConnectionRegistry();
        MatchWebSocketHandler wsHandler = new MatchWebSocketHandler(matchService, connectionRegistry);

        // Create Jetty server
        Server server = new Server(port);

        // Static file handler for client files
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase("client");

        ContextHandler staticContext = new ContextHandler("/");
        staticContext.setHandler(resourceHandler);

        // WebSocket handler
        WebSocketUpgradeHandler wsUpgradeHandler = WebSocketUpgradeHandler.from(server, container -> {
            container.setIdleTimeout(java.time.Duration.ofMinutes(10));
            container.addMapping("/ws", (req, resp) -> new TacticsWebSocketEndpoint(wsHandler, connectionRegistry));
        });

        // Combine handlers
        HandlerList handlers = new HandlerList();
        handlers.addHandler(wsUpgradeHandler);
        handlers.addHandler(staticContext);

        server.setHandler(handlers);

        // Start server
        server.start();
        logger.info("===========================================");
        logger.info("Tactics 5x5 Server started on port {}", port);
        logger.info("WebSocket: ws://localhost:{}/ws", port);
        logger.info("Client: http://localhost:{}/", port);
        logger.info("===========================================");

        server.join();
    }

    private static int getPort(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port '{}', using default {}", args[0], DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
}
