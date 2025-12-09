package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.tactics.client.TacticsGame;

/**
 * Manages screen transitions for the game.
 * Singleton pattern - use getInstance() to access.
 */
public class ScreenManager {

    private static final String TAG = "ScreenManager";

    private static ScreenManager instance;

    private TacticsGame game;
    private BaseScreen currentScreen;

    // Screen instances (lazy initialization)
    private ConnectScreen connectScreen;
    private DraftScreen draftScreen;
    private BattleScreen battleScreen;
    private ResultScreen resultScreen;

    private ScreenManager() {
    }

    public static ScreenManager getInstance() {
        if (instance == null) {
            instance = new ScreenManager();
        }
        return instance;
    }

    /**
     * Initialize the screen manager with the game instance.
     * Must be called before using other methods.
     * @param game The TacticsGame instance
     */
    public void init(TacticsGame game) {
        this.game = game;
    }

    /**
     * Set the current screen.
     * Disposes the previous screen if it's not a reusable screen.
     * @param screen The new screen to display
     */
    public void setScreen(BaseScreen screen) {
        if (currentScreen != null) {
            currentScreen.hide();
            // Don't dispose screens - they might be reused
        }

        currentScreen = screen;
        game.setScreen(screen);
        Gdx.app.log(TAG, "Screen changed to: " + screen.getClass().getSimpleName());
    }

    /**
     * Show the connect screen.
     */
    public void showConnectScreen() {
        if (connectScreen == null) {
            connectScreen = new ConnectScreen(game);
        }
        setScreen(connectScreen);
    }

    /**
     * Show the draft screen.
     */
    public void showDraftScreen() {
        if (draftScreen == null) {
            draftScreen = new DraftScreen(game);
        }
        draftScreen.reset();
        setScreen(draftScreen);
    }

    /**
     * Show the battle screen.
     */
    public void showBattleScreen() {
        if (battleScreen == null) {
            battleScreen = new BattleScreen(game);
        }
        battleScreen.reset();
        setScreen(battleScreen);
    }

    /**
     * Show the result screen.
     * @param isVictory true if player won, false if lost
     * @param winner The winner's ID
     */
    public void showResultScreen(boolean isVictory, String winner) {
        if (resultScreen == null) {
            resultScreen = new ResultScreen(game);
        }
        resultScreen.setResult(isVictory, winner);
        setScreen(resultScreen);
    }

    /**
     * Get the current screen.
     * @return The current screen or null
     */
    public BaseScreen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Get the game instance.
     * @return The TacticsGame instance
     */
    public TacticsGame getGame() {
        return game;
    }

    /**
     * Dispose all screens.
     * Call this when the game is closing.
     */
    public void dispose() {
        if (connectScreen != null) {
            connectScreen.dispose();
            connectScreen = null;
        }
        if (draftScreen != null) {
            draftScreen.dispose();
            draftScreen = null;
        }
        if (battleScreen != null) {
            battleScreen.dispose();
            battleScreen = null;
        }
        if (resultScreen != null) {
            resultScreen.dispose();
            resultScreen = null;
        }
        currentScreen = null;
    }

    /**
     * Clear the singleton instance.
     * For testing purposes.
     */
    public static void clearInstance() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
}
