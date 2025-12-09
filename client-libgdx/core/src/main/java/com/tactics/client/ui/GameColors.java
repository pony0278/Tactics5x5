package com.tactics.client.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Centralized color definitions for the game UI.
 * Prevents duplicate color definitions across screens.
 */
public final class GameColors {

    private GameColors() {
        // Utility class - no instantiation
    }

    // ========== Hero Class Colors ==========
    public static final Color HERO_WARRIOR = new Color(0.8f, 0.3f, 0.3f, 1);   // Red
    public static final Color HERO_MAGE = new Color(0.3f, 0.3f, 0.8f, 1);      // Blue
    public static final Color HERO_ROGUE = new Color(0.3f, 0.6f, 0.3f, 1);     // Green
    public static final Color HERO_CLERIC = new Color(0.8f, 0.8f, 0.3f, 1);    // Yellow
    public static final Color HERO_HUNTRESS = new Color(0.6f, 0.3f, 0.6f, 1);  // Purple
    public static final Color HERO_DUELIST = new Color(0.8f, 0.5f, 0.2f, 1);   // Orange

    /** Get hero color by class name */
    public static Color getHeroColor(String heroClass) {
        if (heroClass == null) return Color.GRAY;
        switch (heroClass.toUpperCase()) {
            case "WARRIOR": return HERO_WARRIOR;
            case "MAGE": return HERO_MAGE;
            case "ROGUE": return HERO_ROGUE;
            case "CLERIC": return HERO_CLERIC;
            case "HUNTRESS": return HERO_HUNTRESS;
            case "DUELIST": return HERO_DUELIST;
            default: return Color.GRAY;
        }
    }

    // ========== Minion Type Colors ==========
    public static final Color MINION_TANK = new Color(0.5f, 0.5f, 0.6f, 1);      // Gray
    public static final Color MINION_ARCHER = new Color(0.4f, 0.6f, 0.4f, 1);    // Light Green
    public static final Color MINION_ASSASSIN = new Color(0.6f, 0.4f, 0.5f, 1);  // Dark Pink

    /** Get minion color by type name */
    public static Color getMinionColor(String minionType) {
        if (minionType == null) return Color.GRAY;
        switch (minionType.toUpperCase()) {
            case "TANK": return MINION_TANK;
            case "ARCHER": return MINION_ARCHER;
            case "ASSASSIN": return MINION_ASSASSIN;
            default: return Color.GRAY;
        }
    }

    // ========== Buff Colors ==========
    public static final Color BUFF_POWER = new Color(0.9f, 0.5f, 0.1f, 1);     // Orange
    public static final Color BUFF_LIFE = new Color(0.2f, 0.7f, 0.2f, 1);      // Green
    public static final Color BUFF_SPEED = new Color(0.9f, 0.9f, 0.2f, 1);     // Yellow
    public static final Color BUFF_WEAKNESS = new Color(0.5f, 0.2f, 0.6f, 1);  // Purple
    public static final Color BUFF_BLEED = new Color(0.6f, 0.1f, 0.1f, 1);     // Dark Red
    public static final Color BUFF_SLOW = new Color(0.4f, 0.4f, 0.4f, 1);      // Gray

    /** Get buff color by buff type name */
    public static Color getBuffColor(String buffType) {
        if (buffType == null) return Color.GRAY;
        switch (buffType.toUpperCase()) {
            case "POWER": return BUFF_POWER;
            case "LIFE": return BUFF_LIFE;
            case "SPEED": return BUFF_SPEED;
            case "WEAKNESS": return BUFF_WEAKNESS;
            case "BLEED": return BUFF_BLEED;
            case "SLOW": return BUFF_SLOW;
            default: return Color.GRAY;
        }
    }

    // ========== Unit Colors (In Battle) ==========
    public static final Color UNIT_ALLY_HERO = new Color(0.2f, 0.4f, 0.8f, 1);      // Blue
    public static final Color UNIT_ALLY_MINION = new Color(0.4f, 0.6f, 0.9f, 1);    // Light Blue
    public static final Color UNIT_ENEMY_HERO = new Color(0.8f, 0.2f, 0.2f, 1);     // Red
    public static final Color UNIT_ENEMY_MINION = new Color(0.9f, 0.5f, 0.5f, 1);   // Pink

    // ========== UI Panel Colors ==========
    public static final Color PANEL_BACKGROUND = new Color(0.15f, 0.15f, 0.2f, 1);
    public static final Color PANEL_BORDER = Color.DARK_GRAY;

    // ========== Button Colors ==========
    public static final Color BUTTON_DISABLED = new Color(0.2f, 0.2f, 0.2f, 1);
    public static final Color BUTTON_MOVE = new Color(0.2f, 0.5f, 0.2f, 1);
    public static final Color BUTTON_ATTACK = new Color(0.5f, 0.2f, 0.2f, 1);
    public static final Color BUTTON_SKILL = new Color(0.3f, 0.2f, 0.5f, 1);
    public static final Color BUTTON_NEUTRAL = new Color(0.3f, 0.3f, 0.3f, 1);
    public static final Color BUTTON_READY = new Color(0.2f, 0.5f, 0.2f, 1);

    // ========== Tile Colors ==========
    public static final Color TILE_EMPTY = new Color(0.2f, 0.2f, 0.25f, 1);
    public static final Color TILE_VALID_MOVE = new Color(0.2f, 0.4f, 0.2f, 0.8f);
    public static final Color TILE_VALID_ATTACK = new Color(0.4f, 0.2f, 0.2f, 0.8f);
    public static final Color TILE_VALID_SKILL = new Color(0.3f, 0.2f, 0.4f, 0.8f);

    // ========== Turn Indicator Colors ==========
    public static final Color TURN_PLAYER = new Color(0.1f, 0.3f, 0.1f, 1);
    public static final Color TURN_OPPONENT = new Color(0.3f, 0.1f, 0.1f, 1);

    // ========== Timer Colors ==========
    public static final Color TIMER_NORMAL = Color.WHITE;
    public static final Color TIMER_WARNING = Color.YELLOW;
    public static final Color TIMER_CRITICAL = Color.RED;
    public static final Color TIMER_BG_NORMAL = new Color(0.2f, 0.2f, 0.25f, 1);
    public static final Color TIMER_BG_CRITICAL = new Color(0.4f, 0.1f, 0.1f, 1);

    // ========== Selection Colors ==========
    public static final Color SELECTION_BORDER = Color.YELLOW;
    public static final Color UNSELECTED_BORDER = new Color(0.4f, 0.4f, 0.4f, 1);

    // ========== HP Bar Colors ==========
    public static final Color HP_HIGH = Color.GREEN;
    public static final Color HP_MEDIUM = Color.YELLOW;
    public static final Color HP_LOW = Color.RED;
    public static final Color HP_BACKGROUND = Color.DARK_GRAY;

    // ========== Background Colors ==========
    public static final Color BG_CONNECT = new Color(0.1f, 0.15f, 0.25f, 1);
    public static final Color BG_DRAFT = new Color(0.12f, 0.14f, 0.18f, 1);
    public static final Color BG_BATTLE = new Color(0.1f, 0.1f, 0.15f, 1);
    public static final Color BG_RESULT_VICTORY = new Color(0.1f, 0.2f, 0.1f, 1);
    public static final Color BG_RESULT_DEFEAT = new Color(0.2f, 0.1f, 0.1f, 1);
}
