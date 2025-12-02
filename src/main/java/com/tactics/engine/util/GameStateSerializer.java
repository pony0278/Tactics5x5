package com.tactics.engine.util;

import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final String KEY_UNIT_BUFFS = "unitBuffs";

    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";

    private static final String KEY_ID = "id";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_HP = "hp";
    private static final String KEY_ATTACK = "attack";
    private static final String KEY_MOVE_RANGE = "moveRange";
    private static final String KEY_ATTACK_RANGE = "attackRange";
    private static final String KEY_POSITION = "position";
    private static final String KEY_ALIVE = "alive";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";

    // Buff-related keys
    private static final String KEY_BUFF_ID = "buffId";
    private static final String KEY_SOURCE_UNIT_ID = "sourceUnitId";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_STACKABLE = "stackable";
    private static final String KEY_MODIFIERS = "modifiers";
    private static final String KEY_FLAGS = "flags";

    // BuffModifier keys
    private static final String KEY_BONUS_HP = "bonusHp";
    private static final String KEY_BONUS_ATTACK = "bonusAttack";
    private static final String KEY_BONUS_MOVE_RANGE = "bonusMoveRange";
    private static final String KEY_BONUS_ATTACK_RANGE = "bonusAttackRange";

    // BuffFlags keys
    private static final String KEY_STUNNED = "stunned";
    private static final String KEY_ROOTED = "rooted";
    private static final String KEY_POISON = "poison";
    private static final String KEY_SILENCED = "silenced";
    private static final String KEY_TAUNTED = "taunted";

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

        // Serialize unitBuffs
        result.put(KEY_UNIT_BUFFS, serializeUnitBuffs(state.getUnitBuffs()));

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

        // Deserialize unitBuffs (optional for forward compatibility)
        Map<String, List<BuffInstance>> unitBuffs = deserializeUnitBuffs(map.get(KEY_UNIT_BUFFS));

        return new GameState(board, units, currentPlayer, gameOver, winner, unitBuffs);
    }

    // =========================================================================
    // Unit Serialization
    // =========================================================================

    private Map<String, Object> serializeUnit(Unit unit) {
        Map<String, Object> unitMap = new HashMap<>();
        unitMap.put(KEY_ID, unit.getId());
        unitMap.put(KEY_OWNER, unit.getOwner().getValue());
        unitMap.put(KEY_HP, unit.getHp());
        unitMap.put(KEY_ATTACK, unit.getAttack());
        unitMap.put(KEY_MOVE_RANGE, unit.getMoveRange());
        unitMap.put(KEY_ATTACK_RANGE, unit.getAttackRange());
        unitMap.put(KEY_ALIVE, unit.isAlive());

        // Serialize position
        Map<String, Object> posMap = new HashMap<>();
        posMap.put(KEY_X, unit.getPosition().getX());
        posMap.put(KEY_Y, unit.getPosition().getY());
        unitMap.put(KEY_POSITION, posMap);

        return unitMap;
    }

    // =========================================================================
    // Buff Serialization
    // =========================================================================

    private Map<String, Object> serializeUnitBuffs(Map<String, List<BuffInstance>> unitBuffs) {
        Map<String, Object> result = new HashMap<>();
        if (unitBuffs == null || unitBuffs.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, List<BuffInstance>> entry : unitBuffs.entrySet()) {
            String unitId = entry.getKey();
            List<BuffInstance> buffs = entry.getValue();

            List<Map<String, Object>> buffList = new ArrayList<>();
            for (BuffInstance buff : buffs) {
                buffList.add(serializeBuffInstance(buff));
            }
            result.put(unitId, buffList);
        }

        return result;
    }

    private Map<String, Object> serializeBuffInstance(BuffInstance buff) {
        Map<String, Object> buffMap = new HashMap<>();
        buffMap.put(KEY_BUFF_ID, buff.getBuffId());
        buffMap.put(KEY_SOURCE_UNIT_ID, buff.getSourceUnitId());
        buffMap.put(KEY_DURATION, buff.getDuration());
        buffMap.put(KEY_STACKABLE, buff.isStackable());
        buffMap.put(KEY_MODIFIERS, serializeBuffModifier(buff.getModifiers()));
        buffMap.put(KEY_FLAGS, serializeBuffFlags(buff.getFlags()));
        return buffMap;
    }

    private Map<String, Object> serializeBuffModifier(BuffModifier modifiers) {
        Map<String, Object> modMap = new HashMap<>();
        modMap.put(KEY_BONUS_HP, modifiers.getBonusHp());
        modMap.put(KEY_BONUS_ATTACK, modifiers.getBonusAttack());
        modMap.put(KEY_BONUS_MOVE_RANGE, modifiers.getBonusMoveRange());
        modMap.put(KEY_BONUS_ATTACK_RANGE, modifiers.getBonusAttackRange());
        return modMap;
    }

    private Map<String, Object> serializeBuffFlags(BuffFlags flags) {
        Map<String, Object> flagsMap = new HashMap<>();
        flagsMap.put(KEY_STUNNED, flags.isStunned());
        flagsMap.put(KEY_ROOTED, flags.isRooted());
        flagsMap.put(KEY_POISON, flags.isPoison());
        flagsMap.put(KEY_SILENCED, flags.isSilenced());
        flagsMap.put(KEY_TAUNTED, flags.isTaunted());
        return flagsMap;
    }

    // =========================================================================
    // Deserialization Helpers
    // =========================================================================

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
        int moveRange = toInt(unitMap.get(KEY_MOVE_RANGE));
        int attackRange = toInt(unitMap.get(KEY_ATTACK_RANGE));
        boolean alive = (Boolean) unitMap.get(KEY_ALIVE);

        Position position = deserializePosition(unitMap.get(KEY_POSITION));

        return new Unit(id, new PlayerId(ownerStr), hp, attack, moveRange, attackRange, position, alive);
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

    // =========================================================================
    // Buff Deserialization
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Map<String, List<BuffInstance>> deserializeUnitBuffs(Object unitBuffsObj) {
        if (unitBuffsObj == null) {
            return Collections.emptyMap();
        }

        if (!(unitBuffsObj instanceof Map)) {
            return Collections.emptyMap();
        }

        Map<String, Object> unitBuffsMap = (Map<String, Object>) unitBuffsObj;
        Map<String, List<BuffInstance>> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : unitBuffsMap.entrySet()) {
            String unitId = entry.getKey();
            Object buffsObj = entry.getValue();

            if (!(buffsObj instanceof List)) {
                continue;
            }

            List<?> buffsList = (List<?>) buffsObj;
            List<BuffInstance> buffs = new ArrayList<>();

            for (Object buffObj : buffsList) {
                if (buffObj instanceof Map) {
                    buffs.add(deserializeBuffInstance((Map<String, Object>) buffObj));
                }
            }

            result.put(unitId, buffs);
        }

        return result;
    }

    private BuffInstance deserializeBuffInstance(Map<String, Object> buffMap) {
        String buffId = (String) buffMap.get(KEY_BUFF_ID);
        String sourceUnitId = (String) buffMap.get(KEY_SOURCE_UNIT_ID);
        int duration = toInt(buffMap.get(KEY_DURATION));
        boolean stackable = toBoolean(buffMap.get(KEY_STACKABLE));

        BuffModifier modifiers = deserializeBuffModifier(buffMap.get(KEY_MODIFIERS));
        BuffFlags flags = deserializeBuffFlags(buffMap.get(KEY_FLAGS));

        return new BuffInstance(buffId, sourceUnitId, duration, stackable, modifiers, flags);
    }

    @SuppressWarnings("unchecked")
    private BuffModifier deserializeBuffModifier(Object modifiersObj) {
        if (!(modifiersObj instanceof Map)) {
            // Default to all zeros if missing
            return new BuffModifier(0, 0, 0, 0);
        }

        Map<String, Object> modMap = (Map<String, Object>) modifiersObj;

        int bonusHp = toIntOrDefault(modMap.get(KEY_BONUS_HP), 0);
        int bonusAttack = toIntOrDefault(modMap.get(KEY_BONUS_ATTACK), 0);
        int bonusMoveRange = toIntOrDefault(modMap.get(KEY_BONUS_MOVE_RANGE), 0);
        int bonusAttackRange = toIntOrDefault(modMap.get(KEY_BONUS_ATTACK_RANGE), 0);

        return new BuffModifier(bonusHp, bonusAttack, bonusMoveRange, bonusAttackRange);
    }

    @SuppressWarnings("unchecked")
    private BuffFlags deserializeBuffFlags(Object flagsObj) {
        if (!(flagsObj instanceof Map)) {
            // Default to all false if missing
            return new BuffFlags(false, false, false, false, false);
        }

        Map<String, Object> flagsMap = (Map<String, Object>) flagsObj;

        boolean stunned = toBooleanOrDefault(flagsMap.get(KEY_STUNNED), false);
        boolean rooted = toBooleanOrDefault(flagsMap.get(KEY_ROOTED), false);
        boolean poison = toBooleanOrDefault(flagsMap.get(KEY_POISON), false);
        boolean silenced = toBooleanOrDefault(flagsMap.get(KEY_SILENCED), false);
        boolean taunted = toBooleanOrDefault(flagsMap.get(KEY_TAUNTED), false);

        return new BuffFlags(stunned, rooted, poison, silenced, taunted);
    }

    // =========================================================================
    // Type Conversion Helpers
    // =========================================================================

    private int toInt(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        throw new IllegalArgumentException("Invalid integer value: " + obj);
    }

    private int toIntOrDefault(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return defaultValue;
    }

    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new IllegalArgumentException("Invalid boolean value: " + obj);
    }

    private boolean toBooleanOrDefault(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return defaultValue;
    }
}
