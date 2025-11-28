package com.tactics.engine.util;

import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert GameState to/from a JSON-friendly map structure.
 */
public class GameStateSerializer {

    private static final String KEY_BOARD = "board";
    private static final String KEY_UNITS = "units";
    private static final String KEY_CURRENT_PLAYER = "currentPlayer";
    private static final String KEY_GAME_OVER = "gameOver";
    private static final String KEY_WINNER = "winner";

    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";

    private static final String KEY_ID = "id";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_HP = "hp";
    private static final String KEY_ATTACK = "attack";
    private static final String KEY_POSITION = "position";
    private static final String KEY_ALIVE = "alive";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";

    public GameStateSerializer() {
    }

    /**
     * Convert a GameState to a JSON-friendly map structure.
     *
     * @param state the GameState to serialize
     * @return a Map representation suitable for JSON serialization
     * @throws IllegalArgumentException if state is null
     */
    public Map<String, Object> toJsonMap(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null");
        }

        Map<String, Object> result = new HashMap<>();

        // Serialize board
        Map<String, Object> boardMap = new HashMap<>();
        boardMap.put(KEY_WIDTH, state.getBoard().getWidth());
        boardMap.put(KEY_HEIGHT, state.getBoard().getHeight());
        result.put(KEY_BOARD, boardMap);

        // Serialize units (preserve order)
        List<Map<String, Object>> unitsList = new ArrayList<>();
        for (Unit unit : state.getUnits()) {
            unitsList.add(serializeUnit(unit));
        }
        result.put(KEY_UNITS, unitsList);

        // Serialize currentPlayer
        result.put(KEY_CURRENT_PLAYER, state.getCurrentPlayer().getValue());

        // Serialize gameOver
        result.put(KEY_GAME_OVER, state.isGameOver());

        // Serialize winner (can be null)
        PlayerId winner = state.getWinner();
        result.put(KEY_WINNER, winner != null ? winner.getValue() : null);

        return result;
    }

    /**
     * Reconstruct a GameState from a JSON-friendly map structure.
     *
     * @param map the map to deserialize
     * @return the reconstructed GameState
     * @throws IllegalArgumentException if map is null or missing required fields
     */
    public GameState fromJsonMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }

        // Validate required keys
        validateRequiredKey(map, KEY_BOARD);
        validateRequiredKey(map, KEY_UNITS);
        validateRequiredKey(map, KEY_CURRENT_PLAYER);
        validateRequiredKey(map, KEY_GAME_OVER);
        // winner key must exist but can be null
        if (!map.containsKey(KEY_WINNER)) {
            throw new IllegalArgumentException("Missing required field: " + KEY_WINNER);
        }

        // Deserialize board
        Board board = deserializeBoard(map.get(KEY_BOARD));

        // Deserialize units (preserve order)
        List<Unit> units = deserializeUnits(map.get(KEY_UNITS));

        // Deserialize currentPlayer
        String currentPlayerStr = (String) map.get(KEY_CURRENT_PLAYER);
        PlayerId currentPlayer = new PlayerId(currentPlayerStr);

        // Deserialize gameOver
        boolean gameOver = (Boolean) map.get(KEY_GAME_OVER);

        // Deserialize winner (can be null)
        Object winnerObj = map.get(KEY_WINNER);
        PlayerId winner = winnerObj != null ? new PlayerId((String) winnerObj) : null;

        return new GameState(board, units, currentPlayer, gameOver, winner);
    }

    private Map<String, Object> serializeUnit(Unit unit) {
        Map<String, Object> unitMap = new HashMap<>();
        unitMap.put(KEY_ID, unit.getId());
        unitMap.put(KEY_OWNER, unit.getOwner().getValue());
        unitMap.put(KEY_HP, unit.getHp());
        unitMap.put(KEY_ATTACK, unit.getAttack());
        unitMap.put(KEY_ALIVE, unit.isAlive());

        // Serialize position
        Map<String, Object> posMap = new HashMap<>();
        posMap.put(KEY_X, unit.getPosition().getX());
        posMap.put(KEY_Y, unit.getPosition().getY());
        unitMap.put(KEY_POSITION, posMap);

        return unitMap;
    }

    private void validateRequiredKey(Map<String, Object> map, String key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
    }

    @SuppressWarnings("unchecked")
    private Board deserializeBoard(Object boardObj) {
        if (!(boardObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid board format: expected Map");
        }
        Map<String, Object> boardMap = (Map<String, Object>) boardObj;

        int width = toInt(boardMap.get(KEY_WIDTH));
        int height = toInt(boardMap.get(KEY_HEIGHT));

        return new Board(width, height);
    }

    @SuppressWarnings("unchecked")
    private List<Unit> deserializeUnits(Object unitsObj) {
        if (!(unitsObj instanceof List)) {
            throw new IllegalArgumentException("Invalid units format: expected List");
        }
        List<?> unitsList = (List<?>) unitsObj;
        List<Unit> units = new ArrayList<>();

        for (Object unitObj : unitsList) {
            if (!(unitObj instanceof Map)) {
                throw new IllegalArgumentException("Invalid unit format: expected Map");
            }
            units.add(deserializeUnit((Map<String, Object>) unitObj));
        }

        return units;
    }

    private Unit deserializeUnit(Map<String, Object> unitMap) {
        String id = (String) unitMap.get(KEY_ID);
        String ownerStr = (String) unitMap.get(KEY_OWNER);
        int hp = toInt(unitMap.get(KEY_HP));
        int attack = toInt(unitMap.get(KEY_ATTACK));
        boolean alive = (Boolean) unitMap.get(KEY_ALIVE);

        Position position = deserializePosition(unitMap.get(KEY_POSITION));

        return new Unit(id, new PlayerId(ownerStr), hp, attack, position, alive);
    }

    @SuppressWarnings("unchecked")
    private Position deserializePosition(Object posObj) {
        if (!(posObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid position format: expected Map");
        }
        Map<String, Object> posMap = (Map<String, Object>) posObj;

        int x = toInt(posMap.get(KEY_X));
        int y = toInt(posMap.get(KEY_Y));

        return new Position(x, y);
    }

    private int toInt(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        throw new IllegalArgumentException("Invalid integer value: " + obj);
    }
}
