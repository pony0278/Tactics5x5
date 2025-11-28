package com.tactics.server.ws;

import com.tactics.server.core.MatchService;

/**
 * Main entry point for WebSocket events; routes messages to MatchService.
 */
public class MatchWebSocketHandler {

    private final MatchService matchService;
    private final ConnectionRegistry connectionRegistry;

    public MatchWebSocketHandler(MatchService matchService,
                                 ConnectionRegistry connectionRegistry) {
        this.matchService = matchService;
        this.connectionRegistry = connectionRegistry;
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public ConnectionRegistry getConnectionRegistry() {
        return connectionRegistry;
    }

    public void onOpen(ClientConnection connection) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void onClose(ClientConnection connection) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void onMessage(ClientConnection connection, String text) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
