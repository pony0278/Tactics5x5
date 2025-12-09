package com.tactics.client.net;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/**
 * Factory for creating platform-specific WebSocket clients.
 * Uses LibGDX Application.getType() for platform detection.
 */
public class WebSocketFactory {

    private static IWebSocketClient instance;
    private static WebSocketClientCreator creator;

    /**
     * Functional interface for creating WebSocket clients.
     * Platform-specific modules should register their creator.
     */
    public interface WebSocketClientCreator {
        IWebSocketClient create();
    }

    /**
     * Register a platform-specific WebSocket client creator.
     * Should be called by platform launchers during initialization.
     *
     * @param creator The creator function for the platform
     */
    public static void registerCreator(WebSocketClientCreator creator) {
        WebSocketFactory.creator = creator;
    }

    /**
     * Create a new WebSocket client for the current platform.
     * The client is created using the registered creator.
     *
     * @return A new IWebSocketClient instance
     * @throws IllegalStateException if no creator is registered
     */
    public static IWebSocketClient create() {
        if (creator == null) {
            throw new IllegalStateException(
                    "No WebSocket client creator registered. " +
                            "Call WebSocketFactory.registerCreator() in your platform launcher.");
        }
        return creator.create();
    }

    /**
     * Get or create a singleton WebSocket client.
     * Useful when only one connection is needed.
     *
     * @return The singleton IWebSocketClient instance
     */
    public static IWebSocketClient getInstance() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    /**
     * Clear the singleton instance.
     * Call this if you need to create a fresh connection.
     */
    public static void clearInstance() {
        if (instance != null) {
            instance.disconnect();
            instance = null;
        }
    }
}
