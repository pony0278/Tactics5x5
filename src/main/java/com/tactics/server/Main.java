package com.tactics.server;

import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.MatchRegistry;
import com.tactics.server.core.MatchService;
import com.tactics.server.ws.ConnectionRegistry;
import com.tactics.server.ws.MatchWebSocketHandler;
import com.tactics.server.ws.TacticsWebSocketEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

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

        // Create servlet context handler for WebSocket
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setIdleTimeout(Duration.ofMinutes(10));
            wsContainer.addMapping("/ws", (req, resp) -> new TacticsWebSocketEndpoint(wsHandler, connectionRegistry));
        });

        // Static file handler for client files
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase("client");

        // Combine handlers - context first for WebSocket, then static files
        HandlerList handlers = new HandlerList();
        handlers.addHandler(context);
        handlers.addHandler(resourceHandler);

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
