package com.tactics.server.ws;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * Jetty WebSocket endpoint that bridges to MatchWebSocketHandler.
 * Implements ClientConnection for framework-agnostic message sending.
 */
public class TacticsWebSocketEndpoint implements WebSocketListener, ClientConnection {

    private static final Logger logger = LoggerFactory.getLogger(TacticsWebSocketEndpoint.class);

    private final String id;
    private final MatchWebSocketHandler handler;
    private final ConnectionRegistry connectionRegistry;
    private Session session;
    private String matchId;
    private String playerId;

    public TacticsWebSocketEndpoint(MatchWebSocketHandler handler, ConnectionRegistry connectionRegistry) {
        this.id = UUID.randomUUID().toString();
        this.handler = handler;
        this.connectionRegistry = connectionRegistry;
    }

    // =========================================================================
    // WebSocketListener Implementation
    // =========================================================================

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        logger.info("WebSocket connected: {}", id);
        handler.onOpen(this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.info("WebSocket closed: {} (code={}, reason={})", id, statusCode, reason);
        handler.onClose(this);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        logger.error("WebSocket error for {}: {}", id, cause.getMessage());
    }

    @Override
    public void onWebSocketText(String message) {
        logger.debug("Received message from {}: {}", id, message);
        try {
            handler.onMessage(this, message);
        } catch (Exception e) {
            logger.error("Error handling message from {}: {}", id, e.getMessage());
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        // Binary messages not supported
        logger.warn("Received unsupported binary message from {}", id);
    }

    // =========================================================================
    // ClientConnection Implementation
    // =========================================================================

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getMatchId() {
        return matchId;
    }

    @Override
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    @Override
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    @Override
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
                logger.debug("Sent message to {}: {}", id, message);
            } catch (IOException e) {
                logger.error("Failed to send message to {}: {}", id, e.getMessage());
            }
        } else {
            logger.warn("Cannot send message to {}: session not open", id);
        }
    }
}
