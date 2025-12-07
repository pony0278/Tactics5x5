package com.tactics.engine.draft;

import com.tactics.engine.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to create GameState from completed draft results.
 *
 * Handles:
 * - Hero creation with selected skill
 * - Minion creation with correct stats
 * - Starting position placement
 * - Initial GameState setup
 */
public class DraftSetupService {

    // Board positions
    private static final int BOARD_SIZE = 5;

    // Player 1 positions (bottom row, y=0)
    private static final Position P1_HERO_POSITION = new Position(2, 0);
    private static final Position P1_MINION_1_POSITION = new Position(0, 0);
    private static final Position P1_MINION_2_POSITION = new Position(4, 0);

    // Player 2 positions (top row, y=4)
    private static final Position P2_HERO_POSITION = new Position(2, 4);
    private static final Position P2_MINION_1_POSITION = new Position(0, 4);
    private static final Position P2_MINION_2_POSITION = new Position(4, 4);

    // Unit stats from UNIT_TYPES_V3.md
    private static final int HERO_HP = 5;
    private static final int HERO_ATTACK = 1;
    private static final int HERO_MOVE_RANGE = 1;
    private static final int HERO_ATTACK_RANGE = 1;

    private static final int TANK_HP = 5;
    private static final int TANK_ATTACK = 1;
    private static final int TANK_MOVE_RANGE = 1;
    private static final int TANK_ATTACK_RANGE = 1;

    private static final int ARCHER_HP = 3;
    private static final int ARCHER_ATTACK = 1;
    private static final int ARCHER_MOVE_RANGE = 1;
    private static final int ARCHER_ATTACK_RANGE = 3;

    private static final int ASSASSIN_HP = 2;
    private static final int ASSASSIN_ATTACK = 2;
    private static final int ASSASSIN_MOVE_RANGE = 4;
    private static final int ASSASSIN_ATTACK_RANGE = 1;

    /**
     * Create a GameState from completed draft results.
     *
     * @param draftResult The completed draft result containing both players' selections
     * @return A new GameState ready for play
     * @throws IllegalArgumentException if draftResult is null or incomplete
     */
    public GameState createGameState(DraftResult draftResult) {
        if (draftResult == null) {
            throw new IllegalArgumentException("DraftResult cannot be null");
        }
        if (!draftResult.isComplete()) {
            throw new IllegalArgumentException("DraftResult must be complete before creating GameState");
        }

        List<Unit> units = new ArrayList<>();

        // Create Player 1 units
        units.add(createHero(PlayerId.PLAYER_1, draftResult.getPlayer1Draft()));
        units.addAll(createMinions(PlayerId.PLAYER_1, draftResult.getPlayer1Draft()));

        // Create Player 2 units
        units.add(createHero(PlayerId.PLAYER_2, draftResult.getPlayer2Draft()));
        units.addAll(createMinions(PlayerId.PLAYER_2, draftResult.getPlayer2Draft()));

        // Create the board (5x5)
        Board board = new Board(BOARD_SIZE, BOARD_SIZE);

        // Create initial GameState
        // Round 1, Player 1 starts, no obstacles, no buff tiles, game not over
        return new GameState(
            board,
            units,
            PlayerId.PLAYER_1,  // Player 1 always starts
            false,              // Not game over
            null                // No winner
        ).withCurrentRound(1);
    }

    /**
     * Create a hero unit for the given player based on their draft.
     */
    private Unit createHero(PlayerId owner, DraftState draft) {
        String id = owner.equals(PlayerId.PLAYER_1) ? "p1_hero" : "p2_hero";
        Position position = owner.equals(PlayerId.PLAYER_1) ? P1_HERO_POSITION : P2_HERO_POSITION;

        return new Unit(
            id,
            owner,
            HERO_HP,
            HERO_ATTACK,
            HERO_MOVE_RANGE,
            HERO_ATTACK_RANGE,
            position,
            true,  // alive
            UnitCategory.HERO,
            null,  // no minion type
            draft.getHeroClass(),
            HERO_HP,  // maxHp
            draft.getSelectedSkillId(),
            0,     // skill cooldown starts at 0
            0,     // no shield
            false, // not invisible
            false, // not invulnerable
            false, // not temporary
            0,     // no temporary duration
            null   // no skill state
        );
    }

    /**
     * Create minion units for the given player based on their draft.
     */
    private List<Unit> createMinions(PlayerId owner, DraftState draft) {
        List<Unit> minions = new ArrayList<>();
        List<MinionType> selectedMinions = draft.getSelectedMinions();

        String prefix = owner.equals(PlayerId.PLAYER_1) ? "p1" : "p2";
        Position pos1 = owner.equals(PlayerId.PLAYER_1) ? P1_MINION_1_POSITION : P2_MINION_1_POSITION;
        Position pos2 = owner.equals(PlayerId.PLAYER_1) ? P1_MINION_2_POSITION : P2_MINION_2_POSITION;

        // First minion at left position
        minions.add(createMinion(
            prefix + "_minion_1",
            owner,
            selectedMinions.get(0),
            pos1
        ));

        // Second minion at right position
        minions.add(createMinion(
            prefix + "_minion_2",
            owner,
            selectedMinions.get(1),
            pos2
        ));

        return minions;
    }

    /**
     * Create a single minion unit with stats based on type.
     */
    private Unit createMinion(String id, PlayerId owner, MinionType type, Position position) {
        int hp, attack, moveRange, attackRange;

        switch (type) {
            case TANK:
                hp = TANK_HP;
                attack = TANK_ATTACK;
                moveRange = TANK_MOVE_RANGE;
                attackRange = TANK_ATTACK_RANGE;
                break;
            case ARCHER:
                hp = ARCHER_HP;
                attack = ARCHER_ATTACK;
                moveRange = ARCHER_MOVE_RANGE;
                attackRange = ARCHER_ATTACK_RANGE;
                break;
            case ASSASSIN:
                hp = ASSASSIN_HP;
                attack = ASSASSIN_ATTACK;
                moveRange = ASSASSIN_MOVE_RANGE;
                attackRange = ASSASSIN_ATTACK_RANGE;
                break;
            default:
                throw new IllegalArgumentException("Unknown minion type: " + type);
        }

        return new Unit(
            id,
            owner,
            hp,
            attack,
            moveRange,
            attackRange,
            position,
            true,  // alive
            UnitCategory.MINION,
            type,
            null,  // no hero class
            hp,    // maxHp
            null,  // no skill
            0,     // no cooldown
            0,     // no shield
            false, // not invisible
            false, // not invulnerable
            false, // not temporary
            0,     // no temporary duration
            null   // no skill state
        );
    }
}
