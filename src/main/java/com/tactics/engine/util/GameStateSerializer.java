package com.tactics.engine.util;

import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.BuffTile;
import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.Obstacle;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert GameState to/from a JSON-friendly map structure.
 * V3 extensions support Unit categories, BuffTiles, Obstacles, etc.
 */
public class GameStateSerializer {

    // V1/V2 Core keys
    private static final String KEY_BOARD = "board";
    private static final String KEY_UNITS = "units";
    private static final String KEY_CURRENT_PLAYER = "currentPlayer";
    private static final String KEY_GAME_OVER = "gameOver";
    private static final String KEY_WINNER = "winner";
    private static final String KEY_UNIT_BUFFS = "unitBuffs";

    // V3 GameState keys
    private static final String KEY_BUFF_TILES = "buffTiles";
    private static final String KEY_OBSTACLES = "obstacles";
    private static final String KEY_CURRENT_ROUND = "currentRound";
    private static final String KEY_PENDING_DEATH_CHOICE = "pendingDeathChoice";
    private static final String KEY_PLAYER1_TURN_ENDED = "player1TurnEnded";
    private static final String KEY_PLAYER2_TURN_ENDED = "player2TurnEnded";

    // Board keys
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";

    // Unit V1/V2 keys
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

    // Unit V3 keys
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_MINION_TYPE = "minionType";
    private static final String KEY_HERO_CLASS = "heroClass";
    private static final String KEY_MAX_HP = "maxHp";
    private static final String KEY_SELECTED_SKILL_ID = "selectedSkillId";
    private static final String KEY_SKILL_COOLDOWN = "skillCooldown";
    private static final String KEY_SHIELD = "shield";
    private static final String KEY_INVISIBLE = "invisible";
    private static final String KEY_INVULNERABLE = "invulnerable";
    private static final String KEY_IS_TEMPORARY = "isTemporary";
    private static final String KEY_TEMPORARY_DURATION = "temporaryDuration";
    private static final String KEY_SKILL_STATE = "skillState";

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

    // BuffFlags keys (V1)
    private static final String KEY_STUNNED = "stunned";
    private static final String KEY_ROOTED = "rooted";
    private static final String KEY_POISON = "poison";
    private static final String KEY_SILENCED = "silenced";
    private static final String KEY_TAUNTED = "taunted";
    // BuffFlags keys (V3)
    private static final String KEY_POWER_BUFF = "powerBuff";
    private static final String KEY_SPEED_BUFF = "speedBuff";
    private static final String KEY_SLOW_BUFF = "slowBuff";
    private static final String KEY_BLEED_BUFF = "bleedBuff";

    // BuffInstance V3 keys
    private static final String KEY_INSTANT_HP_BONUS = "instantHpBonus";

    // Unit action state keys (V3)
    private static final String KEY_ACTIONS_USED = "actionsUsed";
    private static final String KEY_PREPARING = "preparing";
    private static final String KEY_PREPARING_ACTION = "preparingAction";

    // BuffTile keys
    private static final String KEY_BUFF_TYPE = "buffType";
    private static final String KEY_TRIGGERED = "triggered";

    // DeathChoice keys
    private static final String KEY_DEAD_UNIT_ID = "deadUnitId";
    private static final String KEY_DEATH_POSITION = "deathPosition";

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

        // V3: Serialize buffTiles
        List<Map<String, Object>> buffTilesList = new ArrayList<>();
        for (BuffTile tile : state.getBuffTiles()) {
            buffTilesList.add(serializeBuffTile(tile));
        }
        result.put(KEY_BUFF_TILES, buffTilesList);

        // V3: Serialize obstacles
        List<Map<String, Object>> obstaclesList = new ArrayList<>();
        for (Obstacle obstacle : state.getObstacles()) {
            obstaclesList.add(serializeObstacle(obstacle));
        }
        result.put(KEY_OBSTACLES, obstaclesList);

        // V3: Serialize currentRound
        result.put(KEY_CURRENT_ROUND, state.getCurrentRound());

        // V3: Serialize pendingDeathChoice (can be null)
        DeathChoice deathChoice = state.getPendingDeathChoice();
        result.put(KEY_PENDING_DEATH_CHOICE, deathChoice != null ? serializeDeathChoice(deathChoice) : null);

        // V3: Serialize turn ended flags
        result.put(KEY_PLAYER1_TURN_ENDED, state.isPlayer1TurnEnded());
        result.put(KEY_PLAYER2_TURN_ENDED, state.isPlayer2TurnEnded());

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

        // V3: Deserialize buffTiles (optional)
        List<BuffTile> buffTiles = deserializeBuffTiles(map.get(KEY_BUFF_TILES));

        // V3: Deserialize obstacles (optional)
        List<Obstacle> obstacles = deserializeObstacles(map.get(KEY_OBSTACLES));

        // V3: Deserialize currentRound (default to 1)
        int currentRound = toIntOrDefault(map.get(KEY_CURRENT_ROUND), 1);

        // V3: Deserialize pendingDeathChoice (optional)
        DeathChoice pendingDeathChoice = deserializeDeathChoice(map.get(KEY_PENDING_DEATH_CHOICE));

        // V3: Deserialize turn ended flags (default to false)
        boolean player1TurnEnded = toBooleanOrDefault(map.get(KEY_PLAYER1_TURN_ENDED), false);
        boolean player2TurnEnded = toBooleanOrDefault(map.get(KEY_PLAYER2_TURN_ENDED), false);

        return new GameState(board, units, currentPlayer, gameOver, winner, unitBuffs,
                            buffTiles, obstacles, currentRound, pendingDeathChoice,
                            player1TurnEnded, player2TurnEnded);
    }

    // =========================================================================
    // Unit Serialization
    // =========================================================================

    private Map<String, Object> serializeUnit(Unit unit) {
        Map<String, Object> unitMap = new HashMap<>();

        // V1/V2 Core fields
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

        // V3 Category fields (only if set)
        if (unit.getCategory() != null) {
            unitMap.put(KEY_CATEGORY, unit.getCategory().name());
        }
        if (unit.getMinionType() != null) {
            unitMap.put(KEY_MINION_TYPE, unit.getMinionType().name());
        }
        if (unit.getHeroClass() != null) {
            unitMap.put(KEY_HERO_CLASS, unit.getHeroClass().name());
        }
        unitMap.put(KEY_MAX_HP, unit.getMaxHp());

        // V3 Hero Skill fields
        if (unit.getSelectedSkillId() != null) {
            unitMap.put(KEY_SELECTED_SKILL_ID, unit.getSelectedSkillId());
        }
        unitMap.put(KEY_SKILL_COOLDOWN, unit.getSkillCooldown());

        // V3 Skill State fields
        unitMap.put(KEY_SHIELD, unit.getShield());
        unitMap.put(KEY_INVISIBLE, unit.isInvisible());
        unitMap.put(KEY_INVULNERABLE, unit.isInvulnerable());
        unitMap.put(KEY_IS_TEMPORARY, unit.isTemporary());
        unitMap.put(KEY_TEMPORARY_DURATION, unit.getTemporaryDuration());

        if (unit.getSkillState() != null && !unit.getSkillState().isEmpty()) {
            unitMap.put(KEY_SKILL_STATE, new HashMap<>(unit.getSkillState()));
        }

        // V3 Action state fields
        unitMap.put(KEY_ACTIONS_USED, unit.getActionsUsed());
        unitMap.put(KEY_PREPARING, unit.isPreparing());
        if (unit.getPreparingAction() != null) {
            unitMap.put(KEY_PREPARING_ACTION, new HashMap<>(unit.getPreparingAction()));
        }

        return unitMap;
    }

    private Unit deserializeUnit(Map<String, Object> unitMap) {
        // V1/V2 Core fields
        String id = (String) unitMap.get(KEY_ID);
        String ownerStr = (String) unitMap.get(KEY_OWNER);
        int hp = toInt(unitMap.get(KEY_HP));
        int attack = toInt(unitMap.get(KEY_ATTACK));
        int moveRange = toInt(unitMap.get(KEY_MOVE_RANGE));
        int attackRange = toInt(unitMap.get(KEY_ATTACK_RANGE));
        boolean alive = (Boolean) unitMap.get(KEY_ALIVE);
        Position position = deserializePosition(unitMap.get(KEY_POSITION));

        // V3 Category fields
        UnitCategory category = deserializeEnum(unitMap.get(KEY_CATEGORY), UnitCategory.class);
        MinionType minionType = deserializeEnum(unitMap.get(KEY_MINION_TYPE), MinionType.class);
        HeroClass heroClass = deserializeEnum(unitMap.get(KEY_HERO_CLASS), HeroClass.class);
        int maxHp = toIntOrDefault(unitMap.get(KEY_MAX_HP), hp);

        // V3 Hero Skill fields
        String selectedSkillId = (String) unitMap.get(KEY_SELECTED_SKILL_ID);
        int skillCooldown = toIntOrDefault(unitMap.get(KEY_SKILL_COOLDOWN), 0);

        // V3 Skill State fields
        int shield = toIntOrDefault(unitMap.get(KEY_SHIELD), 0);
        boolean invisible = toBooleanOrDefault(unitMap.get(KEY_INVISIBLE), false);
        boolean invulnerable = toBooleanOrDefault(unitMap.get(KEY_INVULNERABLE), false);
        boolean isTemporary = toBooleanOrDefault(unitMap.get(KEY_IS_TEMPORARY), false);
        int temporaryDuration = toIntOrDefault(unitMap.get(KEY_TEMPORARY_DURATION), 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> skillState = (Map<String, Object>) unitMap.get(KEY_SKILL_STATE);

        // V3 Action state fields
        int actionsUsed = toIntOrDefault(unitMap.get(KEY_ACTIONS_USED), 0);
        boolean preparing = toBooleanOrDefault(unitMap.get(KEY_PREPARING), false);

        @SuppressWarnings("unchecked")
        Map<String, Object> preparingAction = (Map<String, Object>) unitMap.get(KEY_PREPARING_ACTION);

        return new Unit(id, new PlayerId(ownerStr), hp, attack, moveRange, attackRange, position, alive,
                       category, minionType, heroClass, maxHp, selectedSkillId, skillCooldown,
                       shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                       actionsUsed, preparing, preparingAction);
    }

    // =========================================================================
    // BuffTile Serialization
    // =========================================================================

    private Map<String, Object> serializeBuffTile(BuffTile tile) {
        Map<String, Object> tileMap = new HashMap<>();
        tileMap.put(KEY_ID, tile.getId());

        Map<String, Object> posMap = new HashMap<>();
        posMap.put(KEY_X, tile.getPosition().getX());
        posMap.put(KEY_Y, tile.getPosition().getY());
        tileMap.put(KEY_POSITION, posMap);

        if (tile.getBuffType() != null) {
            tileMap.put(KEY_BUFF_TYPE, tile.getBuffType().name());
        }
        tileMap.put(KEY_DURATION, tile.getDuration());
        tileMap.put(KEY_TRIGGERED, tile.isTriggered());

        return tileMap;
    }

    @SuppressWarnings("unchecked")
    private List<BuffTile> deserializeBuffTiles(Object buffTilesObj) {
        if (buffTilesObj == null) {
            return Collections.emptyList();
        }
        if (!(buffTilesObj instanceof List)) {
            return Collections.emptyList();
        }

        List<?> tilesList = (List<?>) buffTilesObj;
        List<BuffTile> tiles = new ArrayList<>();

        for (Object tileObj : tilesList) {
            if (tileObj instanceof Map) {
                tiles.add(deserializeBuffTile((Map<String, Object>) tileObj));
            }
        }

        return tiles;
    }

    private BuffTile deserializeBuffTile(Map<String, Object> tileMap) {
        String id = (String) tileMap.get(KEY_ID);
        Position position = deserializePosition(tileMap.get(KEY_POSITION));
        BuffType buffType = deserializeEnum(tileMap.get(KEY_BUFF_TYPE), BuffType.class);
        int duration = toIntOrDefault(tileMap.get(KEY_DURATION), 2);
        boolean triggered = toBooleanOrDefault(tileMap.get(KEY_TRIGGERED), false);

        return new BuffTile(id, position, buffType, duration, triggered);
    }

    // =========================================================================
    // Obstacle Serialization
    // =========================================================================

    private Map<String, Object> serializeObstacle(Obstacle obstacle) {
        Map<String, Object> obstacleMap = new HashMap<>();
        obstacleMap.put(KEY_ID, obstacle.getId());

        Map<String, Object> posMap = new HashMap<>();
        posMap.put(KEY_X, obstacle.getPosition().getX());
        posMap.put(KEY_Y, obstacle.getPosition().getY());
        obstacleMap.put(KEY_POSITION, posMap);

        // V3: Serialize obstacle HP
        obstacleMap.put(KEY_HP, obstacle.getHp());

        return obstacleMap;
    }

    @SuppressWarnings("unchecked")
    private List<Obstacle> deserializeObstacles(Object obstaclesObj) {
        if (obstaclesObj == null) {
            return Collections.emptyList();
        }
        if (!(obstaclesObj instanceof List)) {
            return Collections.emptyList();
        }

        List<?> obstaclesList = (List<?>) obstaclesObj;
        List<Obstacle> obstacles = new ArrayList<>();

        for (Object obstacleObj : obstaclesList) {
            if (obstacleObj instanceof Map) {
                obstacles.add(deserializeObstacle((Map<String, Object>) obstacleObj));
            }
        }

        return obstacles;
    }

    private Obstacle deserializeObstacle(Map<String, Object> obstacleMap) {
        String id = (String) obstacleMap.get(KEY_ID);
        Position position = deserializePosition(obstacleMap.get(KEY_POSITION));
        // V3: Deserialize obstacle HP (default to 3 for backward compatibility)
        int hp = toIntOrDefault(obstacleMap.get(KEY_HP), Obstacle.DEFAULT_HP);

        return new Obstacle(id, position, hp);
    }

    // =========================================================================
    // DeathChoice Serialization
    // =========================================================================

    private Map<String, Object> serializeDeathChoice(DeathChoice deathChoice) {
        Map<String, Object> choiceMap = new HashMap<>();
        choiceMap.put(KEY_DEAD_UNIT_ID, deathChoice.getDeadUnitId());
        choiceMap.put(KEY_OWNER, deathChoice.getOwner().getValue());

        Map<String, Object> posMap = new HashMap<>();
        posMap.put(KEY_X, deathChoice.getDeathPosition().getX());
        posMap.put(KEY_Y, deathChoice.getDeathPosition().getY());
        choiceMap.put(KEY_DEATH_POSITION, posMap);

        return choiceMap;
    }

    @SuppressWarnings("unchecked")
    private DeathChoice deserializeDeathChoice(Object deathChoiceObj) {
        if (deathChoiceObj == null) {
            return null;
        }
        if (!(deathChoiceObj instanceof Map)) {
            return null;
        }

        Map<String, Object> choiceMap = (Map<String, Object>) deathChoiceObj;

        String deadUnitId = (String) choiceMap.get(KEY_DEAD_UNIT_ID);
        String ownerStr = (String) choiceMap.get(KEY_OWNER);
        Position deathPosition = deserializePosition(choiceMap.get(KEY_DEATH_POSITION));

        return new DeathChoice(deadUnitId, new PlayerId(ownerStr), deathPosition);
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
        // V3: BuffType
        if (buff.getType() != null) {
            buffMap.put(KEY_BUFF_TYPE, buff.getType().name());
        }
        buffMap.put(KEY_DURATION, buff.getDuration());
        buffMap.put(KEY_STACKABLE, buff.isStackable());
        buffMap.put(KEY_MODIFIERS, serializeBuffModifier(buff.getModifiers()));
        buffMap.put(KEY_FLAGS, serializeBuffFlags(buff.getFlags()));
        // V3: instantHpBonus
        buffMap.put(KEY_INSTANT_HP_BONUS, buff.getInstantHpBonus());
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
        // V1 flags
        flagsMap.put(KEY_STUNNED, flags.isStunned());
        flagsMap.put(KEY_ROOTED, flags.isRooted());
        flagsMap.put(KEY_POISON, flags.isPoison());
        flagsMap.put(KEY_SILENCED, flags.isSilenced());
        flagsMap.put(KEY_TAUNTED, flags.isTaunted());
        // V3 flags
        flagsMap.put(KEY_POWER_BUFF, flags.isPowerBuff());
        flagsMap.put(KEY_SPEED_BUFF, flags.isSpeedBuff());
        flagsMap.put(KEY_SLOW_BUFF, flags.isSlowBuff());
        flagsMap.put(KEY_BLEED_BUFF, flags.isBleedBuff());
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
        // V3: BuffType
        BuffType buffType = deserializeEnum(buffMap.get(KEY_BUFF_TYPE), BuffType.class);
        int duration = toInt(buffMap.get(KEY_DURATION));
        boolean stackable = toBoolean(buffMap.get(KEY_STACKABLE));

        BuffModifier modifiers = deserializeBuffModifier(buffMap.get(KEY_MODIFIERS));
        BuffFlags flags = deserializeBuffFlags(buffMap.get(KEY_FLAGS));
        // V3: instantHpBonus
        int instantHpBonus = toIntOrDefault(buffMap.get(KEY_INSTANT_HP_BONUS), 0);

        return new BuffInstance(buffId, sourceUnitId, buffType, duration, stackable, modifiers, flags, instantHpBonus);
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
            return new BuffFlags(false, false, false, false, false, false, false, false, false);
        }

        Map<String, Object> flagsMap = (Map<String, Object>) flagsObj;

        // V1 flags
        boolean stunned = toBooleanOrDefault(flagsMap.get(KEY_STUNNED), false);
        boolean rooted = toBooleanOrDefault(flagsMap.get(KEY_ROOTED), false);
        boolean poison = toBooleanOrDefault(flagsMap.get(KEY_POISON), false);
        boolean silenced = toBooleanOrDefault(flagsMap.get(KEY_SILENCED), false);
        boolean taunted = toBooleanOrDefault(flagsMap.get(KEY_TAUNTED), false);
        // V3 flags
        boolean powerBuff = toBooleanOrDefault(flagsMap.get(KEY_POWER_BUFF), false);
        boolean speedBuff = toBooleanOrDefault(flagsMap.get(KEY_SPEED_BUFF), false);
        boolean slowBuff = toBooleanOrDefault(flagsMap.get(KEY_SLOW_BUFF), false);
        boolean bleedBuff = toBooleanOrDefault(flagsMap.get(KEY_BLEED_BUFF), false);

        return new BuffFlags(stunned, rooted, poison, silenced, taunted,
                            powerBuff, speedBuff, slowBuff, bleedBuff);
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

    private <T extends Enum<T>> T deserializeEnum(Object obj, Class<T> enumClass) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            try {
                return Enum.valueOf(enumClass, (String) obj);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
