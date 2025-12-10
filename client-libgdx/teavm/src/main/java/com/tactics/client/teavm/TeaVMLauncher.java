package com.tactics.client.teavm;

import com.github.xpenatan.gdx.backends.teavm.TeaApplication;
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.TeaVMWebSocketClient;
import com.tactics.client.net.WebSocketFactory;

/**
 * TeaVM launcher for web export.
 * Compiles to JavaScript for browser execution.
 *
 * Note: This uses gdx-teavm by xpenatan.
 * To build for web, use: ./gradlew teavm:build
 */
public class TeaVMLauncher {

    public static void main(String[] args) {
        // Register TeaVM WebSocket client creator
        WebSocketFactory.registerCreator(TeaVMWebSocketClient::new);

        TeaApplicationConfiguration config = new TeaApplicationConfiguration("canvas");
        config.width = 800;
        config.height = 600;
        // Assets (font files) will be preloaded before game starts
        new TeaApplication(new TacticsGame(), config);
    }
}
