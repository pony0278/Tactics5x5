package com.tactics.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tactics.client.net.WebSocketFactory;
import com.tactics.client.screens.ScreenManager;

/**
 * Main game class for 5x5 Tactics Client.
 * Manages screens and shared resources.
 */
public class TacticsGame extends Game {

    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        Gdx.app.log("TacticsGame", "Game created");

        // Initialize screen manager
        ScreenManager.getInstance().init(this);

        // Show initial screen (ConnectScreen)
        ScreenManager.getInstance().showConnectScreen();
    }

    @Override
    public void dispose() {
        // Dispose screen manager
        ScreenManager.getInstance().dispose();

        // Clear WebSocket
        WebSocketFactory.clearInstance();

        if (batch != null) {
            batch.dispose();
        }
        super.dispose();
    }
}
