package com.tactics.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.DesktopWebSocketClient;
import com.tactics.client.net.WebSocketFactory;

/**
 * Desktop launcher for 5x5 Tactics Client.
 * Uses LWJGL3 backend.
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        // Register desktop WebSocket client creator
        WebSocketFactory.registerCreator(DesktopWebSocketClient::new);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("5x5 Tactics");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new TacticsGame(), config);
    }
}
