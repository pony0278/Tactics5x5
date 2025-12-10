package com.tactics.client.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader;
import com.google.gwt.core.client.GWT;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.WebSocketFactory;

/**
 * GWT launcher for the Tactics 5x5 web client.
 * Entry point for the browser-based game.
 */
public class GwtLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(true);
        config.padVertical = 0;
        config.padHorizontal = 0;
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        // Register GWT-specific WebSocket client creator
        WebSocketFactory.registerCreator(GwtWebSocketClient::new);
        return new TacticsGame();
    }

    @Override
    public Preloader.PreloaderCallback getPreloaderCallback() {
        // Use default preloader with error logging
        return new Preloader.PreloaderCallback() {
            @Override
            public void error(String file) {
                GWT.log("Preloader error: " + file);
            }

            @Override
            public void update(Preloader.PreloaderState state) {
                // Log progress for debugging
                if (state.hasEnded()) {
                    GWT.log("Preloader finished");
                }
            }
        };
    }
}
