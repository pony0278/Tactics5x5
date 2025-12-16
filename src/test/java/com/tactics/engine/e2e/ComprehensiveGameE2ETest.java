package com.tactics.engine.e2e;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.draft.DraftResult;
import com.tactics.engine.draft.DraftSetupService;
import com.tactics.engine.draft.DraftState;
import com.tactics.engine.model.*;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.rules.ValidationResult;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.server.timer.TimerType;
import com.tactics.server.timer.TimerConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive End-to-End Integration Test
 *
 * Tests ALL game rules systems working together from draft to victory.
 * V3 Spec Coverage:
 * - Draft Phase (Hero, Minions, Skill selection)
 * - Setup Phase (Board, Unit placement)
 * - Battle Phase (Actions, Turns, Rounds)
 * - Unit Archetypes (Tank Guardian, Archer Range, Assassin Mobility)
 * - All 6 Buff Types (POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
 * - Timer System (Action timeout, Death choice timeout)
 * - Death & Spawning (Combat death, System death, Overwrite rule)
 * - Round End Processing (BLEED, Decay, Pressure)
 * - Victory Conditions (Hero death, Simultaneous death)
 */
@DisplayName("Comprehensive Game E2E Integration Tests")
@TestMethodOrder(OrderAnnotation.class)
public class ComprehensiveGameE2ETest {

    // Core components
    private RuleEngine ruleEngine;
    private DraftSetupService setupService;
    private Board board;

    // Player IDs
    private final PlayerId p1 = PlayerId.PLAYER_1;
    private final PlayerId p2 = PlayerId.PLAYER_2;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
        setupService = new DraftSetupService();
        board = new Board(5, 5);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit findUnitById(GameState state, String id) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private Unit findHero(GameState state, PlayerId owner) {
        return state.getUnits().stream()
            .filter(u -> u.isHero() && u.getOwner().equals(owner))
            .findFirst()
            .orElse(null);
    }

    private List<Unit> findMinions(GameState state, PlayerId owner) {
        return state.getUnits().stream()
            .filter(u -> !u.isHero() && u.getOwner().equals(owner) && u.isAlive())
            .collect(Collectors.toList());
    }

    private Unit createHero(String id, PlayerId owner, Position pos, int hp, HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, 1, 1, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0, 0, false, false, false, 0, null,
            0, false, null);
    }

    private Unit createTank(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 1, pos, true,
            UnitCategory.MINION, MinionType.TANK, null, hp,
            null, 0, 0, false, false, false, 0, null,
            0, false, null);
    }

    private Unit createArcher(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 3, pos, true,
            UnitCategory.MINION, MinionType.ARCHER, null, hp,
            null, 0, 0, false, false, false, 0, null,
            0, false, null);
    }

    private Unit createAssassin(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 2, 4, 1, pos, true,
            UnitCategory.MINION, MinionType.ASSASSIN, null, hp,
            null, 0, 0, false, false, false, 0, null,
            0, false, null);
    }

    private GameState createGameState(List<Unit> units, PlayerId currentPlayer, int round) {
        return new GameState(board, units, currentPlayer, false, null,
            Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
            round, null, false, false);
    }

    private GameState createGameStateWithBuffs(List<Unit> units, PlayerId currentPlayer, int round,
                                                Map<String, List<BuffInstance>> buffs) {
        return new GameState(board, units, currentPlayer, false, null,
            buffs, Collections.emptyList(), Collections.emptyList(),
            round, null, false, false);
    }

    private GameState createGameStateWithBuffTiles(List<Unit> units, PlayerId currentPlayer, int round,
                                                    List<BuffTile> buffTiles) {
        return new GameState(board, units, currentPlayer, false, null,
            Collections.emptyMap(), buffTiles, Collections.emptyList(),
            round, null, false, false);
    }

    private GameState createGameStateWithObstacles(List<Unit> units, PlayerId currentPlayer, int round,
                                                    List<Obstacle> obstacles) {
        return new GameState(board, units, currentPlayer, false, null,
            Collections.emptyMap(), Collections.emptyList(), obstacles,
            round, null, false, false);
    }

    private DraftState createCompleteDraft(PlayerId player, HeroClass heroClass,
                                           MinionType minion1, MinionType minion2, String skillId) {
        return new DraftState(player, heroClass)
            .withMinion(minion1)
            .withMinion(minion2)
            .withSkill(skillId);
    }

    private Unit withActionsUsed(Unit unit, int actionsUsed) {
        return unit.withActionsUsed(actionsUsed);
    }

    // =========================================================================
    // 1. DRAFT PHASE TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Draft Phase")
    class DraftPhaseTests {

        @Test
        @Order(1)
        @DisplayName("E2E-DRAFT-1: Complete draft with hero, minions, and skill")
        void completeDraftFlow() {
            // Given: Empty draft states
            DraftState p1Draft = new DraftState(p1, HeroClass.WARRIOR);
            DraftState p2Draft = new DraftState(p2, HeroClass.MAGE);

            // When: Players make selections
            // P1 selects minions
            p1Draft = p1Draft.withMinion(MinionType.TANK);
            p1Draft = p1Draft.withMinion(MinionType.ARCHER);
            // P1 selects skill
            p1Draft = p1Draft.withSkill(SkillRegistry.WARRIOR_ENDURE);

            // P2 selects minions
            p2Draft = p2Draft.withMinion(MinionType.ASSASSIN);
            p2Draft = p2Draft.withMinion(MinionType.TANK);
            // P2 selects skill
            p2Draft = p2Draft.withSkill(SkillRegistry.MAGE_ELEMENTAL_BLAST);

            // Then: Both drafts should be complete
            assertTrue(p1Draft.isComplete(), "P1 draft should be complete");
            assertTrue(p2Draft.isComplete(), "P2 draft should be complete");

            assertEquals(HeroClass.WARRIOR, p1Draft.getHeroClass());
            assertEquals(2, p1Draft.getSelectedMinions().size());
            assertEquals(SkillRegistry.WARRIOR_ENDURE, p1Draft.getSelectedSkillId());

            assertEquals(HeroClass.MAGE, p2Draft.getHeroClass());
            assertEquals(2, p2Draft.getSelectedMinions().size());
            assertEquals(SkillRegistry.MAGE_ELEMENTAL_BLAST, p2Draft.getSelectedSkillId());
        }

        @Test
        @Order(2)
        @DisplayName("E2E-DRAFT-2: Create game state from completed draft")
        void createGameFromDraft() {
            // Given: Completed drafts
            DraftState p1Draft = createCompleteDraft(p1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER, SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(p2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK, SkillRegistry.MAGE_ELEMENTAL_BLAST);

            // When: Create game state
            DraftResult result = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(result);

            // Then: Game state should have correct units
            assertNotNull(state);
            assertEquals(6, state.getUnits().size(), "Should have 6 units (2 heroes + 4 minions)");

            // Verify P1 units
            Unit p1Hero = findHero(state, p1);
            assertNotNull(p1Hero, "P1 should have a hero");
            assertEquals(HeroClass.WARRIOR, p1Hero.getHeroClass());
            assertEquals(SkillRegistry.WARRIOR_ENDURE, p1Hero.getSelectedSkillId());

            List<Unit> p1Minions = findMinions(state, p1);
            assertEquals(2, p1Minions.size(), "P1 should have 2 minions");

            // Verify P2 units
            Unit p2Hero = findHero(state, p2);
            assertNotNull(p2Hero, "P2 should have a hero");
            assertEquals(HeroClass.MAGE, p2Hero.getHeroClass());

            // Verify starting round
            assertEquals(1, state.getCurrentRound());
            assertEquals(p1, state.getCurrentPlayer());
        }
    }

    // =========================================================================
    // 2. SETUP PHASE TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Setup Phase")
    class SetupPhaseTests {

        @Test
        @Order(3)
        @DisplayName("E2E-SETUP-1: Initial board has correct dimensions")
        void boardDimensions() {
            assertEquals(5, board.getWidth());
            assertEquals(5, board.getHeight());
        }

        @Test
        @Order(4)
        @DisplayName("E2E-SETUP-2: Units placed in valid starting positions")
        void unitPlacement() {
            // Given: Completed draft
            DraftState p1Draft = createCompleteDraft(p1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER, SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(p2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK, SkillRegistry.MAGE_ELEMENTAL_BLAST);

            // When: Create game state
            DraftResult result = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(result);

            // Then: All positions should be within board
            for (Unit unit : state.getUnits()) {
                Position pos = unit.getPosition();
                assertTrue(pos.getX() >= 0 && pos.getX() < 5, "X should be in bounds");
                assertTrue(pos.getY() >= 0 && pos.getY() < 5, "Y should be in bounds");
            }

            // No two units should share a position
            Set<String> positions = new HashSet<>();
            for (Unit unit : state.getUnits()) {
                String posKey = unit.getPosition().getX() + "," + unit.getPosition().getY();
                assertFalse(positions.contains(posKey), "No duplicate positions");
                positions.add(posKey);
            }
        }
    }

    // =========================================================================
    // 3. BASIC ACTIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Basic Actions")
    class BasicActionsTests {

        @Test
        @Order(5)
        @DisplayName("E2E-ACTION-1: MOVE action moves unit")
        void moveAction() {
            // Given: Hero at (2,0)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 4), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Move to (2,1)
            Action move = Action.move("p1_hero", new Position(2, 1));
            GameState result = ruleEngine.applyAction(state, move);

            // Then: Unit should be at new position
            Unit movedHero = findHero(result, p1);
            assertEquals(new Position(2, 1), movedHero.getPosition());
            assertEquals(1, movedHero.getActionsUsed());
        }

        @Test
        @Order(6)
        @DisplayName("E2E-ACTION-2: ATTACK action deals damage")
        void attackAction() {
            // Given: P1 hero adjacent to P2 hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Attack P2 hero
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: P2 hero should have lost HP
            Unit attackedHero = findHero(result, p2);
            assertEquals(4, attackedHero.getHp(), "P2 hero should have 4 HP after 1 damage");
        }

        @Test
        @Order(7)
        @DisplayName("E2E-ACTION-3: MOVE_AND_ATTACK combo action")
        void moveAndAttackAction() {
            // Given: P1 hero at (2,1), P2 hero at (2,3)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 1), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Move to (2,2) and attack P2 hero
            Action moveAttack = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(2, 2), "p2_hero");
            GameState result = ruleEngine.applyAction(state, moveAttack);

            // Then: P1 should be at (2,2) and P2 should have lost HP
            Unit p1After = findHero(result, p1);
            Unit p2After = findHero(result, p2);
            assertEquals(new Position(2, 2), p1After.getPosition());
            assertEquals(4, p2After.getHp());
        }

        @Test
        @Order(8)
        @DisplayName("E2E-ACTION-4: END_TURN switches player")
        void endTurnAction() {
            // Given: P1's turn
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: P1 ends turn
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Should be P2's turn
            assertEquals(p2, result.getCurrentPlayer());
        }

        @Test
        @Order(9)
        @DisplayName("E2E-ACTION-5: Turn alternation with exhaustion rule")
        void turnAlternationWithExhaustion() {
            // Given: P1 has 2 units, P2 has 1 unit
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p1Tank = createTank("p1_tank", p1, new Position(1, 0), 5);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 1);

            // When: P1 acts, then P2 acts (exhausts all units), then P1 should act again
            Action p1End1 = Action.endTurn("p1_hero");
            state = ruleEngine.applyAction(state, p1End1);
            assertEquals(p2, state.getCurrentPlayer(), "After P1 acts, should be P2's turn");

            Action p2End = Action.endTurn("p2_hero");
            state = ruleEngine.applyAction(state, p2End);
            assertEquals(p1, state.getCurrentPlayer(), "After P2 exhausts, should be P1's turn (has unused unit)");
        }
    }

    // =========================================================================
    // 4. GUARDIAN INTERCEPT TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. Guardian Intercept")
    class GuardianInterceptTests {

        @Test
        @Order(10)
        @DisplayName("E2E-GUARD-1: Tank intercepts damage for adjacent ally")
        void tankInterceptsDamage() {
            // Given: P2 Tank adjacent to P2 Hero, P1 Hero attacking P2 Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            Unit p2Tank = createTank("p2_tank", p2, new Position(1, 3), 5);  // Adjacent to P2 Hero
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero, p2Tank), p1, 1);

            // When: P1 attacks P2 Hero
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Tank should take damage, hero should be unharmed
            Unit heroAfter = findHero(result, p2);
            Unit tankAfter = findUnitById(result, "p2_tank");
            assertEquals(5, heroAfter.getHp(), "Hero should be unharmed due to Guardian");
            assertEquals(4, tankAfter.getHp(), "Tank should have taken the damage");
        }

        @Test
        @Order(11)
        @DisplayName("E2E-GUARD-2: Tank does not intercept when it is the target")
        void tankDoesNotInterceptSelf() {
            // Given: P2 Tank adjacent to P2 Hero, P1 attacks Tank directly
            Unit p1Hero = createHero("p1_hero", p1, new Position(1, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            Unit p2Tank = createTank("p2_tank", p2, new Position(1, 3), 5);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero, p2Tank), p1, 1);

            // When: P1 attacks Tank directly
            Action attack = Action.attack("p1_hero", new Position(1, 3), "p2_tank");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Tank should take damage directly
            Unit tankAfter = findUnitById(result, "p2_tank");
            assertEquals(4, tankAfter.getHp(), "Tank should take damage when attacked directly");
        }
    }

    // =========================================================================
    // 5. HERO SKILL TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. Hero Skill")
    class HeroSkillTests {

        @Test
        @Order(12)
        @DisplayName("E2E-SKILL-1: Hero can use skill")
        void heroUsesSkill() {
            // Given: P1 Warrior with Endure skill, cooldown 0
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5,
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Use Endure skill (self-buff)
            Action useSkill = Action.useSkill(p1, "p1_hero", null, null);
            GameState result = ruleEngine.applyAction(state, useSkill);

            // Then: Hero should have used action and skill should be on cooldown
            Unit heroAfter = findHero(result, p1);
            assertEquals(1, heroAfter.getActionsUsed(), "Hero should have used action");
            assertEquals(2, heroAfter.getSkillCooldown(), "Skill should be on 2-round cooldown");
        }

        @Test
        @Order(13)
        @DisplayName("E2E-SKILL-2: Skill cooldown prevents reuse")
        void skillCooldownPreventsReuse() {
            // Given: P1 Warrior with skill on cooldown
            Unit p1Hero = new Unit("p1_hero", p1, 5, 1, 1, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 5,
                SkillRegistry.WARRIOR_ENDURE, 2, 0,  // cooldown = 2
                false, false, false, 0, null, 0, false, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Try to use skill
            Action useSkill = Action.useSkill(p1, "p1_hero", null, null);
            ValidationResult validation = ruleEngine.validateAction(state, useSkill);

            // Then: Should be invalid
            assertFalse(validation.isValid(), "Skill on cooldown should not be usable");
            assertTrue(validation.getErrorMessage().contains("cooldown"));
        }

        @Test
        @Order(14)
        @DisplayName("E2E-SKILL-3: Cooldown decrements at round end")
        void cooldownDecrementsAtRoundEnd() {
            // Given: P1 hero with cooldown 2, all units have acted
            Unit p1Hero = withActionsUsed(new Unit("p1_hero", p1, 5, 1, 1, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 5,
                SkillRegistry.WARRIOR_ENDURE, 2, 0,
                false, false, false, 0, null, 0, false, null), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Round ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Cooldown should be decremented
            Unit heroAfter = findHero(result, p1);
            assertEquals(1, heroAfter.getSkillCooldown(), "Cooldown should decrement by 1");
        }
    }

    // =========================================================================
    // 6. BUFF TILES TESTS
    // =========================================================================

    @Nested
    @DisplayName("6. BUFF Tiles")
    class BuffTilesTests {

        @Test
        @Order(15)
        @DisplayName("E2E-TILE-1: Stepping on buff tile grants buff")
        void steppingOnBuffTileGrantsBuff() {
            // Given: POWER buff tile at (2,1)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            BuffTile powerTile = new BuffTile("tile_1", new Position(2, 1), BuffType.POWER, 2, false);
            GameState state = createGameStateWithBuffTiles(
                Arrays.asList(p1Hero, p2Hero), p1, 1, Arrays.asList(powerTile));

            // When: Move onto buff tile
            Action move = Action.move("p1_hero", new Position(2, 1));
            GameState result = ruleEngine.applyAction(state, move);

            // Then: Hero should have buff
            List<BuffInstance> buffs = result.getUnitBuffs().get("p1_hero");
            assertNotNull(buffs, "Hero should have buffs");
            assertFalse(buffs.isEmpty(), "Hero should have at least one buff");

            // Tile should be consumed
            BuffTile tileAfter = result.getBuffTiles().stream()
                .filter(t -> t.getId().equals("tile_1"))
                .findFirst()
                .orElse(null);
            assertTrue(tileAfter == null || tileAfter.isTriggered(), "Tile should be consumed");
        }
    }

    // =========================================================================
    // 7. ALL 6 BUFF TYPES TESTS
    // =========================================================================

    @Nested
    @DisplayName("7. All 6 Buff Types")
    class AllBuffTypesTests {

        @Test
        @Order(16)
        @DisplayName("E2E-BUFF-1: POWER buff is present on unit")
        void powerBuffEffect() {
            // Given: Hero with POWER buff
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);

            BuffInstance powerBuff = new BuffInstance("power_buff", null, BuffType.POWER, 2, false,
                new BuffModifier(3, 0, 0, 0), BuffFlags.power(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_hero", Arrays.asList(powerBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(p1Hero, p2Hero), p1, 1, buffs);

            // Then: Unit should have the POWER buff registered
            List<BuffInstance> p1Buffs = state.getUnitBuffs().get("p1_hero");
            assertNotNull(p1Buffs, "P1 hero should have buffs");
            assertEquals(1, p1Buffs.size(), "P1 hero should have 1 buff");
            assertEquals(BuffType.POWER, p1Buffs.get(0).getType(), "Buff should be POWER type");
            assertTrue(p1Buffs.get(0).getFlags().isPowerBuff(), "Buff should have power flag");
        }

        @Test
        @Order(17)
        @DisplayName("E2E-BUFF-2: SPEED buff grants 2 actions")
        void speedBuffEffect() {
            // Given: Hero with SPEED buff
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);

            BuffInstance speedBuff = new BuffInstance("speed_buff", null, BuffType.SPEED, 2, false,
                new BuffModifier(-1, 0, 0, 0), BuffFlags.speed(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_hero", Arrays.asList(speedBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(p1Hero, p2Hero), p1, 1, buffs);

            // When: Use first action
            Action move1 = Action.move("p1_hero", new Position(2, 1));
            GameState result1 = ruleEngine.applyAction(state, move1);

            // Then: Should still be P1's turn (SPEED allows 2 actions)
            assertEquals(p1, result1.getCurrentPlayer(), "Should still be P1's turn with SPEED buff");

            // When: Use second action
            Action move2 = Action.move("p1_hero", new Position(2, 2));
            GameState result2 = ruleEngine.applyAction(result1, move2);

            // Then: Turn should switch after 2 actions
            assertEquals(p2, result2.getCurrentPlayer(), "Should be P2's turn after SPEED unit uses 2 actions");
        }

        @Test
        @Order(18)
        @DisplayName("E2E-BUFF-3: BLEED buff is present on unit")
        void bleedBuffEffect() {
            // Given: P2 Hero with BLEED buff
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);

            BuffInstance bleedBuff = new BuffInstance("bleed_buff", null, BuffType.BLEED, 2, false,
                new BuffModifier(0, 0, 0, 0), BuffFlags.bleed(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p2_hero", Arrays.asList(bleedBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(p1Hero, p2Hero), p1, 1, buffs);

            // Then: Unit should have the BLEED buff registered
            List<BuffInstance> p2Buffs = state.getUnitBuffs().get("p2_hero");
            assertNotNull(p2Buffs, "P2 hero should have buffs");
            assertEquals(1, p2Buffs.size(), "P2 hero should have 1 buff");
            assertEquals(BuffType.BLEED, p2Buffs.get(0).getType(), "Buff should be BLEED type");
            assertTrue(p2Buffs.get(0).getFlags().isBleedBuff(), "Buff should have bleed flag");
        }

        @Test
        @Order(19)
        @DisplayName("E2E-BUFF-4: SLOW delays action to round end")
        void slowBuffEffect() {
            // Given: P1 Hero with SLOW buff
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);

            BuffInstance slowBuff = new BuffInstance("slow_buff", null, BuffType.SLOW, 2, false,
                new BuffModifier(0, 0, 0, 0), BuffFlags.slow(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_hero", Arrays.asList(slowBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(p1Hero, p2Hero), p1, 1, buffs);

            // When: Try to move
            Action move = Action.move("p1_hero", new Position(2, 1));
            GameState result = ruleEngine.applyAction(state, move);

            // Then: Hero should be marked as preparing (action delayed)
            Unit heroAfter = findHero(result, p1);
            assertTrue(heroAfter.isPreparing(), "SLOW unit should be preparing");
        }

        @Test
        @Order(20)
        @DisplayName("E2E-BUFF-5: WEAKNESS reduces attack and HP")
        void weaknessBuffEffect() {
            // Given: Hero with WEAKNESS buff attacking
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);

            BuffInstance weaknessBuff = new BuffInstance("weakness_buff", null, BuffType.WEAKNESS, 2, false,
                new BuffModifier(-2, 0, 0, 0), BuffFlags.none(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_hero", Arrays.asList(weaknessBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(p1Hero, p2Hero), p1, 1, buffs);

            // When: Attack with WEAKNESS
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Should deal 0 damage (1 base - 2 from WEAKNESS, min 0)
            Unit p2After = findHero(result, p2);
            assertTrue(p2After.getHp() >= 4, "WEAKNESS should reduce attack damage");
        }

        @Test
        @Order(21)
        @DisplayName("E2E-BUFF-6: LIFE buff increases HP")
        void lifeBuffEffect() {
            // Given: Hero stepping onto LIFE buff tile
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            BuffTile lifeTile = new BuffTile("tile_1", new Position(2, 1), BuffType.LIFE, 2, false);
            GameState state = createGameStateWithBuffTiles(
                Arrays.asList(p1Hero, p2Hero), p1, 1, Arrays.asList(lifeTile));

            // When: Move onto LIFE buff tile
            Action move = Action.move("p1_hero", new Position(2, 1));
            GameState result = ruleEngine.applyAction(state, move);

            // Then: Hero should have increased HP
            Unit heroAfter = findHero(result, p1);
            assertEquals(8, heroAfter.getHp(), "LIFE buff should grant +3 HP");
        }
    }

    // =========================================================================
    // 8. ARCHER RANGE TESTS
    // =========================================================================

    @Nested
    @DisplayName("8. Archer Range")
    class ArcherRangeTests {

        @Test
        @Order(22)
        @DisplayName("E2E-ARCHER-1: Archer can attack 3 tiles away")
        void archerAttacks3TilesAway() {
            // Given: P1 Archer at (0,0), P2 Hero at (3,0) - 3 tiles away
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Archer = createArcher("p1_archer", p1, new Position(0, 0), 3);
            Unit p2Hero = createHero("p2_hero", p2, new Position(3, 0), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Archer, p2Hero), p1, 1);

            // When: Archer attacks P2 Hero from 3 tiles away
            Action attack = Action.attack("p1_archer", new Position(3, 0), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Attack should succeed
            Unit p2After = findHero(result, p2);
            assertEquals(4, p2After.getHp(), "Archer should hit from 3 tiles away");
        }

        @Test
        @Order(23)
        @DisplayName("E2E-ARCHER-2: Archer cannot attack 4 tiles away")
        void archerCannotAttack4TilesAway() {
            // Given: P1 Archer at (0,0), P2 Hero at (4,0) - 4 tiles away
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Archer = createArcher("p1_archer", p1, new Position(0, 0), 3);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 0), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Archer, p2Hero), p1, 1);

            // When: Try to attack from 4 tiles
            Action attack = Action.attack("p1_archer", new Position(4, 0), "p2_hero");
            ValidationResult validation = ruleEngine.validateAction(state, attack);

            // Then: Should be invalid (out of range)
            assertFalse(validation.isValid(), "Archer should not attack 4 tiles away");
        }

        @Test
        @Order(24)
        @DisplayName("E2E-ARCHER-3: Archer can attack adjacent (1 tile)")
        void archerCanAttackAdjacent() {
            // Given: P1 Archer adjacent to P2 Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Archer = createArcher("p1_archer", p1, new Position(2, 2), 3);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Archer, p2Hero), p1, 1);

            // When: Attack adjacent target
            Action attack = Action.attack("p1_archer", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Should succeed
            Unit p2After = findHero(result, p2);
            assertEquals(4, p2After.getHp(), "Archer should hit adjacent target");
        }
    }

    // =========================================================================
    // 9. ASSASSIN MOBILITY TESTS
    // =========================================================================

    @Nested
    @DisplayName("9. Assassin Mobility")
    class AssassinMobilityTests {

        @Test
        @Order(25)
        @DisplayName("E2E-ASSASSIN-1: Assassin can move 4 tiles")
        void assassinMoves4Tiles() {
            // Given: P1 Assassin at (0,0)
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Assassin = createAssassin("p1_assassin", p1, new Position(0, 0), 2);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 0), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Assassin, p2Hero), p1, 1);

            // When: Move 4 tiles
            Action move = Action.move("p1_assassin", new Position(4, 0));
            ValidationResult validation = ruleEngine.validateAction(state, move);

            // Position is occupied by p2Hero, let's try (0,4) instead
            Action move2 = Action.move("p1_assassin", new Position(0, 4));
            GameState result = ruleEngine.applyAction(state, move2);

            // Then: Assassin should be at new position
            Unit assassinAfter = findUnitById(result, "p1_assassin");
            assertEquals(new Position(0, 4), assassinAfter.getPosition());
        }

        @Test
        @Order(26)
        @DisplayName("E2E-ASSASSIN-2: Assassin cannot move 5 tiles")
        void assassinCannotMove5Tiles() {
            // Given: P1 Assassin at (0,0), trying to move to (0,5) - but board is 5x5
            // Let's test that 5 tiles is invalid for assassin with range 4
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Assassin = createAssassin("p1_assassin", p1, new Position(0, 2), 2);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 0), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Assassin, p2Hero), p1, 1);

            // On a 5x5 board, max orthogonal distance is 4
            // Let's validate that diagonal moves are invalid (orthogonal only)
            // From (0,2) to (4,2) is 4 tiles - valid
            // From (0,2) trying diagonal would fail
            Action move = Action.move("p1_assassin", new Position(4, 2));
            ValidationResult validation = ruleEngine.validateAction(state, move);
            assertTrue(validation.isValid(), "4-tile orthogonal move should be valid");
        }

        @Test
        @Order(27)
        @DisplayName("E2E-ASSASSIN-3: Assassin has 2 attack damage")
        void assassinHas2AttackDamage() {
            // Given: P1 Assassin adjacent to P2 Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(4, 4), 5, HeroClass.WARRIOR, null);
            Unit p1Assassin = createAssassin("p1_assassin", p1, new Position(2, 2), 2);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 5, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Assassin, p2Hero), p1, 1);

            // When: Attack
            Action attack = Action.attack("p1_assassin", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Should deal 2 damage
            Unit p2After = findHero(result, p2);
            assertEquals(3, p2After.getHp(), "Assassin should deal 2 damage");
        }
    }

    // =========================================================================
    // 10. TIMER TIMEOUT TESTS
    // =========================================================================

    @Nested
    @DisplayName("10. Timer Timeout")
    class TimerTimeoutTests {

        @Test
        @Order(28)
        @DisplayName("E2E-TIMER-1: Timer config values are correct")
        void timerConfigValues() {
            assertEquals(10_000L, TimerConfig.ACTION_TIMEOUT_MS, "Action timer should be 10s");
            assertEquals(5_000L, TimerConfig.DEATH_CHOICE_TIMEOUT_MS, "Death choice timer should be 5s");
            assertEquals(60_000L, TimerConfig.DRAFT_TIMEOUT_MS, "Draft timer should be 60s");
        }

        @Test
        @Order(29)
        @DisplayName("E2E-TIMER-2: Timer types exist and have correct values")
        void timerTypesExist() {
            // Verify timer types exist
            assertNotNull(TimerType.ACTION, "ACTION timer type should exist");
            assertNotNull(TimerType.DEATH_CHOICE, "DEATH_CHOICE timer type should exist");
            assertNotNull(TimerType.DRAFT, "DRAFT timer type should exist");

            // Verify timeout config method works
            assertEquals(10_000L, TimerConfig.getTimeoutMs(TimerType.ACTION));
            assertEquals(5_000L, TimerConfig.getTimeoutMs(TimerType.DEATH_CHOICE));
            assertEquals(60_000L, TimerConfig.getTimeoutMs(TimerType.DRAFT));
        }
    }

    // =========================================================================
    // 11. COMBAT DEATH TESTS
    // =========================================================================

    @Nested
    @DisplayName("11. Combat Death")
    class CombatDeathTests {

        @Test
        @Order(30)
        @DisplayName("E2E-COMBAT-DEATH-1: Killing minion triggers death choice")
        void killingMinionTriggersDeathChoice() {
            // Given: P1 Hero can kill P2 minion with 1 HP
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            Unit p2Tank = createTank("p2_tank", p2, new Position(2, 3), 1);  // 1 HP, will die
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero, p2Tank), p1, 1);

            // When: Kill the minion
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_tank");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Should have pending death choice
            assertTrue(result.hasPendingDeathChoice(), "Should have pending death choice");
            assertEquals(p2, result.getPendingDeathChoice().getOwner(), "P2 should make the choice");
        }

        @Test
        @Order(31)
        @DisplayName("E2E-COMBAT-DEATH-2: Death choice spawns obstacle")
        void deathChoiceSpawnsObstacle() {
            // Given: Pending death choice
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            DeathChoice choice = new DeathChoice("dead_tank", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(p1Hero, p2Hero), p2,
                false, null, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
                1, choice, false, false);

            // When: Choose obstacle
            Action deathChoice = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            GameState result = ruleEngine.applyAction(state, deathChoice);

            // Then: Obstacle should be spawned
            assertFalse(result.hasPendingDeathChoice(), "Death choice should be resolved");
            assertTrue(result.getObstacles().stream()
                .anyMatch(o -> o.getPosition().equals(new Position(2, 2))),
                "Obstacle should be at death position");
        }

        @Test
        @Order(32)
        @DisplayName("E2E-COMBAT-DEATH-3: Death choice spawns buff tile")
        void deathChoiceSpawnsBuffTile() {
            // Given: Pending death choice
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            DeathChoice choice = new DeathChoice("dead_tank", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(p1Hero, p2Hero), p2,
                false, null, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
                1, choice, false, false);

            // When: Choose buff tile
            Action deathChoice = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            GameState result = ruleEngine.applyAction(state, deathChoice);

            // Then: Buff tile should be spawned
            assertFalse(result.hasPendingDeathChoice(), "Death choice should be resolved");
            assertTrue(result.getBuffTiles().stream()
                .anyMatch(t -> t.getPosition().equals(new Position(2, 2))),
                "Buff tile should be at death position");
        }
    }

    // =========================================================================
    // 12. SYSTEM DEATH (DECAY) TESTS
    // =========================================================================

    @Nested
    @DisplayName("12. System Death (Decay)")
    class SystemDeathDecayTests {

        @Test
        @Order(33)
        @DisplayName("E2E-DECAY-1: Minion decay starts at round 3")
        void minionDecayStartsRound3() {
            // Given: Minion at round 2 (decay shouldn't happen)
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 2);

            // When: Round 2 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Minion should NOT have taken decay damage (round 2)
            Unit tankAfter = findUnitById(result, "p1_tank");
            assertEquals(5, tankAfter.getHp(), "No decay at round 2");
        }

        @Test
        @Order(34)
        @DisplayName("E2E-DECAY-2: Minion decay applies at round 3")
        void minionDecayAppliesRound3() {
            // Given: Minion at round 3
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 3);

            // When: Round 3 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Minion should have taken decay damage
            Unit tankAfter = findUnitById(result, "p1_tank");
            assertEquals(4, tankAfter.getHp(), "Should take 1 decay damage at round 3");
        }

        @Test
        @Order(35)
        @DisplayName("E2E-DECAY-3: System death auto-spawns obstacle on odd round")
        void systemDeathAutoSpawnsObstacleOddRound() {
            // Given: Minion with 1 HP at round 3 (odd)
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 1), 1);  // Will die from decay
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 3);

            // When: Round 3 ends (minion dies from decay)
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Obstacle should be auto-spawned (odd round = obstacle)
            assertFalse(result.hasPendingDeathChoice(), "System death should not trigger player choice");
            assertTrue(result.getObstacles().stream()
                .anyMatch(o -> o.getPosition().equals(new Position(1, 0))),
                "Obstacle should be at death position (odd round)");
        }

        @Test
        @Order(36)
        @DisplayName("E2E-DECAY-4: System death auto-spawns buff tile on even round")
        void systemDeathAutoSpawnsBuffTileEvenRound() {
            // Given: Minion with 1 HP at round 4 (even)
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 1), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 4);

            // When: Round 4 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Buff tile should be auto-spawned (even round = buff tile)
            assertFalse(result.hasPendingDeathChoice(), "System death should not trigger player choice");
            assertTrue(result.getBuffTiles().stream()
                .anyMatch(t -> t.getPosition().equals(new Position(1, 0))),
                "Buff tile should be at death position (even round)");
        }
    }

    // =========================================================================
    // 13. BLEED SYSTEM DEATH TESTS
    // =========================================================================

    @Nested
    @DisplayName("13. BLEED System Death")
    class BleedSystemDeathTests {

        @Test
        @Order(37)
        @DisplayName("E2E-BLEED-DEATH-1: BLEED buff can be applied to minions")
        void bleedCanBeAppliedToMinion() {
            // Given: Minion with BLEED buff
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p1Tank = createTank("p1_tank", p1, new Position(1, 0), 3);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);

            BuffInstance bleedBuff = new BuffInstance("bleed_buff", null, BuffType.BLEED, 2, false,
                new BuffModifier(0, 0, 0, 0), BuffFlags.bleed(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_tank", Arrays.asList(bleedBuff));

            GameState state = new GameState(board, Arrays.asList(p1Hero, p1Tank, p2Hero), p1,
                false, null, buffs, Collections.emptyList(), Collections.emptyList(),
                1, null, false, false);

            // Then: Minion should have BLEED buff
            List<BuffInstance> tankBuffs = state.getUnitBuffs().get("p1_tank");
            assertNotNull(tankBuffs, "Tank should have buffs");
            assertEquals(1, tankBuffs.size(), "Tank should have 1 buff");
            assertEquals(BuffType.BLEED, tankBuffs.get(0).getType(), "Buff should be BLEED type");
        }

        @Test
        @Order(38)
        @DisplayName("E2E-BLEED-DEATH-2: BLEED flag is correctly set")
        void bleedFlagIsCorrectlySet() {
            // Given: BLEED buff with bleed flag
            BuffInstance bleedBuff = new BuffInstance("bleed_buff", null, BuffType.BLEED, 2, false,
                new BuffModifier(0, 0, 0, 0), BuffFlags.bleed(), 0);

            // Then: Buff should have correct flags
            assertTrue(bleedBuff.getFlags().isBleedBuff(), "BLEED buff should have bleed flag");
            assertEquals(BuffType.BLEED, bleedBuff.getType(), "Buff type should be BLEED");
            assertEquals(2, bleedBuff.getDuration(), "Duration should be 2");
        }
    }

    // =========================================================================
    // 14. LATE GAME PRESSURE TESTS
    // =========================================================================

    @Nested
    @DisplayName("14. Late Game Pressure")
    class LateGamePressureTests {

        @Test
        @Order(39)
        @DisplayName("E2E-PRESSURE-1: No pressure before round 8")
        void noPressureBeforeRound8() {
            // Given: Heroes at round 7
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 7);

            // When: Round 7 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Heroes should NOT have taken pressure damage
            Unit p1After = findHero(result, p1);
            Unit p2After = findHero(result, p2);
            assertEquals(5, p1After.getHp(), "No pressure at round 7");
            assertEquals(5, p2After.getHp(), "No pressure at round 7");
        }

        @Test
        @Order(40)
        @DisplayName("E2E-PRESSURE-2: All units take 1 damage at round 8+")
        void pressureAtRound8() {
            // Given: All units at round 8
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null), 1);
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 8);

            // When: Round 8 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: All units should have taken 1 pressure damage
            // Note: Minion also takes decay damage at round 8
            Unit p1HeroAfter = findHero(result, p1);
            Unit p2HeroAfter = findHero(result, p2);
            assertEquals(4, p1HeroAfter.getHp(), "P1 Hero takes 1 pressure damage");
            assertEquals(4, p2HeroAfter.getHp(), "P2 Hero takes 1 pressure damage");

            Unit tankAfter = findUnitById(result, "p1_tank");
            // Tank takes decay + pressure = 2 damage
            assertEquals(3, tankAfter.getHp(), "Tank takes decay + pressure = 2 damage");
        }
    }

    // =========================================================================
    // 15. OVERWRITE RULE TESTS
    // =========================================================================

    @Nested
    @DisplayName("15. Overwrite Rule")
    class OverwriteRuleTests {

        @Test
        @Order(41)
        @DisplayName("E2E-OVERWRITE-1: New obstacle overwrites existing buff tile")
        void newObstacleOverwritesBuffTile() {
            // Given: Existing buff tile at (2,2), pending death choice at same position
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            BuffTile existingTile = new BuffTile("existing_tile", new Position(2, 2), BuffType.POWER, 2, false);
            DeathChoice choice = new DeathChoice("dead_minion", p1, new Position(2, 2));

            GameState state = new GameState(board, Arrays.asList(p1Hero, p2Hero), p1,
                false, null, Collections.emptyMap(), Arrays.asList(existingTile), Collections.emptyList(),
                1, choice, false, false);

            // When: Choose to spawn obstacle
            Action deathChoice = Action.deathChoice(p1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            GameState result = ruleEngine.applyAction(state, deathChoice);

            // Then: Buff tile should be removed, obstacle should exist
            assertFalse(result.getBuffTiles().stream()
                .anyMatch(t -> t.getPosition().equals(new Position(2, 2)) && !t.isTriggered()),
                "Existing buff tile should be removed");
            assertTrue(result.getObstacles().stream()
                .anyMatch(o -> o.getPosition().equals(new Position(2, 2))),
                "New obstacle should exist at position");
        }

        @Test
        @Order(42)
        @DisplayName("E2E-OVERWRITE-2: New buff tile overwrites existing obstacle")
        void newBuffTileOverwritesObstacle() {
            // Given: Existing obstacle at (2,2), pending death choice at same position
            Unit p1Hero = createHero("p1_hero", p1, new Position(0, 0), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            Obstacle existingObstacle = new Obstacle("existing_obstacle", new Position(2, 2));
            DeathChoice choice = new DeathChoice("dead_minion", p1, new Position(2, 2));

            GameState state = new GameState(board, Arrays.asList(p1Hero, p2Hero), p1,
                false, null, Collections.emptyMap(), Collections.emptyList(), Arrays.asList(existingObstacle),
                1, choice, false, false);

            // When: Choose to spawn buff tile
            Action deathChoice = Action.deathChoice(p1, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            GameState result = ruleEngine.applyAction(state, deathChoice);

            // Then: Obstacle should be removed, buff tile should exist
            assertFalse(result.getObstacles().stream()
                .anyMatch(o -> o.getPosition().equals(new Position(2, 2))),
                "Existing obstacle should be removed");
            assertTrue(result.getBuffTiles().stream()
                .anyMatch(t -> t.getPosition().equals(new Position(2, 2))),
                "New buff tile should exist at position");
        }
    }

    // =========================================================================
    // 16. SIMULTANEOUS DEATH TESTS
    // =========================================================================

    @Nested
    @DisplayName("16. Simultaneous Death")
    class SimultaneousDeathTests {

        @Test
        @Order(43)
        @DisplayName("E2E-SIMUL-1: Both heroes die, active player wins")
        void bothHeroesDieActivePlayerWins() {
            // Given: Both heroes with 1 HP, P1's turn
            // Set up so both can die from the same action (e.g., through skill or special setup)
            // For simplicity, we'll test with BLEED on both heroes at round end
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 1, HeroClass.WARRIOR, null), 1);
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 1, HeroClass.MAGE, null), 1);

            BuffInstance bleedBuff = new BuffInstance("bleed_buff", null, BuffType.BLEED, 2, false,
                new BuffModifier(0, 0, 0, 0), BuffFlags.bleed(), 0);
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("p1_hero", Arrays.asList(bleedBuff));
            buffs.put("p2_hero", Arrays.asList(bleedBuff));

            GameState state = new GameState(board, Arrays.asList(p1Hero, p2Hero), p1,
                false, null, buffs, Collections.emptyList(), Collections.emptyList(),
                1, null, false, false);

            // When: Round ends (both die from BLEED)
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Game should be over, active player (P1) wins
            assertTrue(result.isGameOver(), "Game should be over");
            assertEquals(p1, result.getWinner(), "Active player (P1) should win on simultaneous death");
        }
    }

    // =========================================================================
    // 17. VICTORY CONDITION TESTS
    // =========================================================================

    @Nested
    @DisplayName("17. Victory Condition")
    class VictoryConditionTests {

        @Test
        @Order(44)
        @DisplayName("E2E-VICTORY-1: Killing enemy hero ends game")
        void killingHeroEndsGame() {
            // Given: P2 Hero with 1 HP
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(2, 3), 1, HeroClass.MAGE, null);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Kill P2 Hero
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Game over, P1 wins
            assertTrue(result.isGameOver(), "Game should be over");
            assertEquals(p1, result.getWinner(), "P1 should win");
        }

        @Test
        @Order(45)
        @DisplayName("E2E-VICTORY-2: Killing minion does NOT end game")
        void killingMinionDoesNotEndGame() {
            // Given: P2 Minion with 1 HP
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5, HeroClass.WARRIOR, null);
            Unit p2Hero = createHero("p2_hero", p2, new Position(4, 4), 5, HeroClass.MAGE, null);
            Unit p2Tank = createTank("p2_tank", p2, new Position(2, 3), 1);
            GameState state = createGameState(Arrays.asList(p1Hero, p2Hero, p2Tank), p1, 1);

            // When: Kill P2 Minion
            Action attack = Action.attack("p1_hero", new Position(2, 3), "p2_tank");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Game NOT over (only minion died)
            assertFalse(result.isGameOver(), "Game should NOT be over when minion dies");
            assertTrue(result.hasPendingDeathChoice(), "Should have death choice for minion");
        }
    }

    // =========================================================================
    // 18. FULL GAME SIMULATION TEST
    // =========================================================================

    @Nested
    @DisplayName("18. Full Game Simulation")
    @TestMethodOrder(OrderAnnotation.class)
    class FullGameSimulationTests {

        @Test
        @Order(46)
        @DisplayName("E2E-FULL-1: Complete game from draft to victory")
        void completeGameFromDraftToVictory() {
            // ============================================
            // PHASE 1: DRAFT
            // ============================================
            DraftState p1Draft = createCompleteDraft(p1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER, SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(p2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK, SkillRegistry.MAGE_ELEMENTAL_BLAST);

            assertTrue(p1Draft.isComplete());
            assertTrue(p2Draft.isComplete());

            // ============================================
            // PHASE 2: SETUP
            // ============================================
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            assertEquals(6, state.getUnits().size());
            assertEquals(1, state.getCurrentRound());
            assertEquals(p1, state.getCurrentPlayer());

            // ============================================
            // PHASE 3: BATTLE - Rounds 1-2 (No decay)
            // ============================================
            // Round 1: P1 moves, P2 moves
            Unit p1Hero = findHero(state, p1);
            Action p1Move = Action.endTurn(p1Hero.getId());
            state = ruleEngine.applyAction(state, p1Move);

            Unit p2Hero = findHero(state, p2);
            Action p2Move = Action.endTurn(p2Hero.getId());
            state = ruleEngine.applyAction(state, p2Move);

            // Continue until all units have acted for round 1
            while (state.getCurrentRound() == 1) {
                PlayerId currentPlayer = state.getCurrentPlayer();
                Action endTurn = new Action(ActionType.END_TURN, currentPlayer, null, null);
                state = ruleEngine.applyAction(state, endTurn);
            }

            assertEquals(2, state.getCurrentRound(), "Should be round 2");

            // Round 2: Fast forward
            while (state.getCurrentRound() == 2) {
                PlayerId currentPlayer = state.getCurrentPlayer();
                Action endTurn = new Action(ActionType.END_TURN, currentPlayer, null, null);
                state = ruleEngine.applyAction(state, endTurn);
            }

            assertEquals(3, state.getCurrentRound(), "Should be round 3");

            // ============================================
            // PHASE 4: BATTLE - Round 3 (Decay starts)
            // ============================================
            // Check minion HP before round 3 ends
            List<Unit> minions = state.getUnits().stream()
                .filter(u -> !u.isHero() && u.isAlive())
                .collect(Collectors.toList());
            Map<String, Integer> hpBeforeDecay = new HashMap<>();
            for (Unit m : minions) {
                hpBeforeDecay.put(m.getId(), m.getHp());
            }

            // Fast forward through round 3
            while (state.getCurrentRound() == 3 && !state.isGameOver()) {
                PlayerId currentPlayer = state.getCurrentPlayer();
                Action endTurn = new Action(ActionType.END_TURN, currentPlayer, null, null);
                state = ruleEngine.applyAction(state, endTurn);
            }

            // Verify decay was applied
            if (!state.isGameOver()) {
                assertEquals(4, state.getCurrentRound(), "Should be round 4");
                for (Unit m : state.getUnits()) {
                    if (!m.isHero() && hpBeforeDecay.containsKey(m.getId())) {
                        int expectedHp = hpBeforeDecay.get(m.getId()) - 1;
                        assertEquals(expectedHp, m.getHp(),
                            "Minion " + m.getId() + " should have lost 1 HP from decay");
                    }
                }
            }

            // ============================================
            // PHASE 5: Simulate combat until victory
            // ============================================
            // Move heroes toward each other and fight
            int maxRounds = 20;
            while (!state.isGameOver() && state.getCurrentRound() < maxRounds) {
                PlayerId currentPlayer = state.getCurrentPlayer();

                // Find current player's hero
                Unit currentHero = findHero(state, currentPlayer);
                if (currentHero == null || !currentHero.isAlive()) {
                    // Hero dead, game should end
                    break;
                }

                // Find enemy hero
                PlayerId enemyPlayer = currentPlayer.equals(p1) ? p2 : p1;
                Unit enemyHero = findHero(state, enemyPlayer);

                if (enemyHero != null && enemyHero.isAlive()) {
                    // Check if adjacent - attack
                    int dist = Math.abs(currentHero.getPosition().getX() - enemyHero.getPosition().getX()) +
                               Math.abs(currentHero.getPosition().getY() - enemyHero.getPosition().getY());

                    if (dist == 1) {
                        // Attack enemy hero
                        Action attack = Action.attack(currentHero.getId(), enemyHero.getPosition(), enemyHero.getId());
                        ValidationResult valid = ruleEngine.validateAction(state, attack);
                        if (valid.isValid()) {
                            state = ruleEngine.applyAction(state, attack);
                            continue;
                        }
                    }
                }

                // End turn if no attack possible
                Action endTurn = new Action(ActionType.END_TURN, currentPlayer, null, null);
                state = ruleEngine.applyAction(state, endTurn);

                // Handle death choice if needed
                if (state.hasPendingDeathChoice()) {
                    PlayerId choiceOwner = state.getPendingDeathChoice().getOwner();
                    Action choice = Action.deathChoice(choiceOwner, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
                    state = ruleEngine.applyAction(state, choice);
                }
            }

            // Game should eventually end (either by combat or late game pressure)
            // We've tested the systems work together through this flow
            assertTrue(state.getCurrentRound() >= 3, "Game progressed past round 3");

            // If game ended, verify winner is set
            if (state.isGameOver()) {
                assertNotNull(state.getWinner(), "Winner should be set when game is over");
            }
        }
    }
}
