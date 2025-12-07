package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes hero skill implementations.
 * Extracted from RuleEngine to improve code organization.
 *
 * Contains all applySkill* methods for Phase 4A-4D skills.
 */
public class SkillExecutor {

    private RngProvider rngProvider;

    public SkillExecutor() {
        this.rngProvider = new RngProvider();
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
    }

    // =========================================================================
    // Functional Interface for Unit Transformation
    // =========================================================================

    @FunctionalInterface
    private interface UnitTransformer {
        Unit transform(Unit unit);
    }

    // =========================================================================
    // Game Over Result
    // =========================================================================

    private static class GameOverResult {
        final boolean isGameOver;
        final PlayerId winner;

        GameOverResult(boolean isGameOver, PlayerId winner) {
            this.isGameOver = isGameOver;
            this.winner = winner;
        }
    }

    // =========================================================================
    // Main Entry Point
    // =========================================================================

    /**
     * Execute a skill and return the new game state.
     */
    public GameState executeSkill(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        switch (skill.getSkillId()) {
            // Phase 4A Skills
            case SkillRegistry.WARRIOR_ENDURE:
                return applySkillEndure(state, action, actingUnit, skill);
            case SkillRegistry.HUNTRESS_SPIRIT_HAWK:
                return applySkillSpiritHawk(state, action, actingUnit, skill);

            // Phase 4B Skills
            case SkillRegistry.MAGE_ELEMENTAL_BLAST:
                return applySkillElementalBlast(state, action, actingUnit, skill);
            case SkillRegistry.WARRIOR_SHOCKWAVE:
                return applySkillShockwave(state, action, actingUnit, skill);
            case SkillRegistry.HUNTRESS_NATURES_POWER:
                return applySkillNaturesPower(state, action, actingUnit, skill);
            case SkillRegistry.CLERIC_TRINITY:
                return applySkillTrinity(state, action, actingUnit, skill);
            case SkillRegistry.CLERIC_POWER_OF_MANY:
                return applySkillPowerOfMany(state, action, actingUnit, skill);

            // Phase 4C Skills
            case SkillRegistry.WARRIOR_HEROIC_LEAP:
                return applySkillHeroicLeap(state, action, actingUnit, skill);
            case SkillRegistry.HUNTRESS_SPECTRAL_BLADES:
                return applySkillSpectralBlades(state, action, actingUnit, skill);
            case SkillRegistry.ROGUE_SMOKE_BOMB:
                return applySkillSmokeBomb(state, action, actingUnit, skill);
            case SkillRegistry.MAGE_WARP_BEACON:
                return applySkillWarpBeacon(state, action, actingUnit, skill);

            // Phase 4D Skills
            case SkillRegistry.MAGE_WILD_MAGIC:
                return applySkillWildMagic(state, action, actingUnit, skill);
            case SkillRegistry.DUELIST_ELEMENTAL_STRIKE:
                return applySkillElementalStrike(state, action, actingUnit, skill);
            case SkillRegistry.ROGUE_DEATH_MARK:
                return applySkillDeathMark(state, action, actingUnit, skill);
            case SkillRegistry.CLERIC_ASCENDED_FORM:
                return applySkillAscendedForm(state, action, actingUnit, skill);
            case SkillRegistry.ROGUE_SHADOW_CLONE:
                return applySkillShadowClone(state, action, actingUnit, skill);
            case SkillRegistry.DUELIST_FEINT:
                return applySkillFeint(state, action, actingUnit, skill);
            case SkillRegistry.DUELIST_CHALLENGE:
                return applySkillChallenge(state, action, actingUnit, skill);

            default:
                // Placeholder for unimplemented skills
                return applySkillPlaceholder(state, action, actingUnit, skill);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    private List<Unit> updateUnitInList(List<Unit> units, String unitId, UnitTransformer transformer) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                newUnits.add(transformer.transform(u));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    private List<Unit> updateUnitsInList(List<Unit> units, Map<String, UnitTransformer> transformers) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            UnitTransformer transformer = transformers.get(u.getId());
            if (transformer != null) {
                newUnits.add(transformer.transform(u));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    private boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
    }

    private boolean isInBounds(Position pos, Board board) {
        return pos.getX() >= 0 && pos.getX() < board.getWidth() &&
               pos.getY() >= 0 && pos.getY() < board.getHeight();
    }

    private boolean isTileOccupied(List<Unit> units, Position pos) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().getX() == pos.getX() &&
                u.getPosition().getY() == pos.getY()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasObstacleAt(GameState state, Position pos) {
        return state.hasObstacleAt(pos);
    }

    private boolean isTileBlocked(GameState state, Position pos) {
        return isTileOccupied(state.getUnits(), pos) || hasObstacleAt(state, pos);
    }

    private GameOverResult checkGameOver(List<Unit> units) {
        return checkGameOver(units, null);
    }

    private GameOverResult checkGameOver(List<Unit> units, PlayerId activePlayer) {
        boolean p1HasAlive = false;
        boolean p2HasAlive = false;

        for (Unit u : units) {
            if (u.isAlive()) {
                if (u.getOwner().isPlayer1()) {
                    p1HasAlive = true;
                } else {
                    p2HasAlive = true;
                }
            }
        }

        // V3: Simultaneous death - active player wins
        if (!p1HasAlive && !p2HasAlive) {
            if (activePlayer != null) {
                return new GameOverResult(true, activePlayer);
            }
            return new GameOverResult(true, PlayerId.PLAYER_1);
        }

        if (!p1HasAlive) {
            return new GameOverResult(true, PlayerId.PLAYER_2);
        } else if (!p2HasAlive) {
            return new GameOverResult(true, PlayerId.PLAYER_1);
        }
        return new GameOverResult(false, null);
    }

    /**
     * Find the Guardian (TANK) that will intercept damage for the target unit.
     */
    private Unit findGuardian(GameState state, Unit target) {
        if (target == null) {
            return null;
        }

        Unit guardian = null;

        for (Unit u : state.getUnits()) {
            if (!u.isAlive()) {
                continue;
            }
            if (!u.getOwner().getValue().equals(target.getOwner().getValue())) {
                continue;
            }
            if (u.getMinionType() != MinionType.TANK) {
                continue;
            }
            if (u.getId().equals(target.getId())) {
                continue;
            }
            if (!isAdjacent(u.getPosition(), target.getPosition())) {
                continue;
            }

            if (guardian == null || u.getId().compareTo(guardian.getId()) < 0) {
                guardian = u;
            }
        }

        return guardian;
    }

    // =========================================================================
    // Buff Helper Methods
    // =========================================================================

    private Map<String, List<BuffInstance>> removeBleedBuffs(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return unitBuffs;
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return unitBuffs;
        }

        List<BuffInstance> remainingBuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() == null || !buff.getFlags().isBleedBuff()) {
                remainingBuffs.add(buff);
            }
        }

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        if (remainingBuffs.isEmpty()) {
            newUnitBuffs.remove(unitId);
        } else {
            newUnitBuffs.put(unitId, remainingBuffs);
        }

        return newUnitBuffs;
    }

    private boolean isDebuff(BuffInstance buff) {
        if (buff.getFlags() == null) {
            return false;
        }
        return buff.getFlags().isBleedBuff() ||
               buff.getFlags().isSlowBuff() ||
               buff.getFlags().isStunned() ||
               buff.getFlags().isRooted() ||
               (buff.getModifiers() != null && buff.getModifiers().getBonusAttack() < 0);
    }

    private Map<String, List<BuffInstance>> removeOneRandomDebuff(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return new HashMap<>();
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        List<BuffInstance> debuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (isDebuff(buff)) {
                debuffs.add(buff);
            }
        }

        if (debuffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        BuffInstance toRemove = debuffs.get(rngProvider.nextInt(debuffs.size()));

        List<BuffInstance> remainingBuffs = new ArrayList<>();
        boolean removed = false;
        for (BuffInstance buff : buffs) {
            if (!removed && buff.getBuffId().equals(toRemove.getBuffId())) {
                removed = true;
                continue;
            }
            remainingBuffs.add(buff);
        }

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        if (remainingBuffs.isEmpty()) {
            newUnitBuffs.remove(unitId);
        } else {
            newUnitBuffs.put(unitId, remainingBuffs);
        }

        return newUnitBuffs;
    }

    /**
     * Create a custom ATK buff for Power of Many (no instant HP bonus).
     */
    private BuffInstance createAtkBuff(String sourceUnitId, int bonusAtk, int duration) {
        return new BuffInstance(
            "power_of_many_" + System.currentTimeMillis(),
            sourceUnitId,
            duration,
            true,
            new BuffModifier(0, bonusAtk, 0, 0),  // bonusHp, bonusAttack, bonusMoveRange, bonusAttackRange
            null
        );
    }

    // =========================================================================
    // Placeholder Skill
    // =========================================================================

    private GameState applySkillPlaceholder(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(skill.getCooldown()));
        return state.withUnits(newUnits);
    }

    // =========================================================================
    // Phase 4A Skills
    // =========================================================================

    /**
     * Apply Warrior Endure skill.
     * Effect: Gain 3 shield for 2 rounds, remove BLEED debuff.
     */
    private GameState applySkillEndure(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int shieldAmount = skill.getShieldAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withShieldAndSkillUsed(u.getShield() + shieldAmount, cooldown));

        Map<String, List<BuffInstance>> newUnitBuffs = removeBleedBuffs(state.getUnitBuffs(), actingUnit.getId());

        GameOverResult gameOver = checkGameOver(newUnits);

        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Huntress Spirit Hawk skill.
     * Effect: Deal 2 damage to enemy at long range (4 tiles).
     */
    private GameState applySkillSpiritHawk(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 2
        int cooldown = skill.getCooldown();  // 2

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());

        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    // =========================================================================
    // Phase 4B Skills
    // =========================================================================

    /**
     * Apply Mage Elemental Blast skill.
     * Effect: Deal 3 damage to target, 50% chance to apply random debuff (WEAKNESS, BLEED, or SLOW).
     */
    private GameState applySkillElementalBlast(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // 50% chance to apply random debuff
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        if (rngProvider.nextInt(100) < 50) {
            BuffType[] debuffs = {BuffType.WEAKNESS, BuffType.BLEED, BuffType.SLOW};
            BuffType debuffType = debuffs[rngProvider.nextInt(3)];
            BuffInstance debuff = BuffFactory.create(debuffType, actingUnit.getId());

            List<BuffInstance> targetBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(damageReceiverId, Collections.emptyList())
            );
            targetBuffs.add(debuff);
            newUnitBuffs.put(damageReceiverId, targetBuffs);

            // Apply instant HP effects for WEAKNESS (-1 HP)
            if (debuff.getInstantHpBonus() != 0) {
                List<Unit> unitsWithDebuffHp = new ArrayList<>();
                for (Unit u : newUnits) {
                    if (u.getId().equals(damageReceiverId)) {
                        unitsWithDebuffHp.add(u.withHpBonus(debuff.getInstantHpBonus()));
                    } else {
                        unitsWithDebuffHp.add(u);
                    }
                }
                newUnits = unitsWithDebuffHp;
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Warrior Shockwave skill.
     * Effect: Deal 1 damage to all adjacent enemies and push them 1 tile away.
     * If enemy cannot be pushed (blocked), deal +1 damage instead.
     */
    private GameState applySkillShockwave(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        Position heroPos = actingUnit.getPosition();

        // Find all adjacent enemies
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(heroPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        adjacentEnemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track units being moved and damaged
        Map<String, Position> newPositions = new HashMap<>();
        Map<String, Integer> damageAmounts = new HashMap<>();

        for (Unit enemy : adjacentEnemies) {
            // Calculate push direction (away from hero)
            int dx = enemy.getPosition().getX() - heroPos.getX();
            int dy = enemy.getPosition().getY() - heroPos.getY();
            Position pushDest = new Position(
                enemy.getPosition().getX() + dx,
                enemy.getPosition().getY() + dy
            );

            // Check if push destination is valid
            boolean canPush = isInBounds(pushDest, state.getBoard()) &&
                              !isTileBlocked(state, pushDest) &&
                              !newPositions.containsValue(pushDest);

            // Check Guardian intercept
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();

            if (canPush) {
                newPositions.put(enemy.getId(), pushDest);
                damageAmounts.merge(damageReceiverId, damage, Integer::sum);
            } else {
                damageAmounts.merge(damageReceiverId, damage + 1, Integer::sum);
            }
        }

        // Apply all changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else if (newPositions.containsKey(u.getId())) {
                int totalDamage = damageAmounts.getOrDefault(u.getId(), 0);
                newUnits.add(u.withPosition(newPositions.get(u.getId())).withDamage(totalDamage));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Huntress Nature's Power skill.
     * Effect: Next 2 attacks deal +2 damage, gain LIFE buff (+3 HP instant).
     */
    private GameState applySkillNaturesPower(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2
        int bonusDamage = skill.getDamageAmount();  // 2 (bonus per attack)
        int attackCharges = 2;

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsedAndBonusAttack(cooldown, bonusDamage, attackCharges));

        // Apply LIFE buff (+3 HP instant)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance lifeBuff = BuffFactory.create(BuffType.LIFE, actingUnit.getId());
        List<BuffInstance> heroBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        heroBuffs.add(lifeBuff);
        newUnitBuffs.put(actingUnit.getId(), heroBuffs);

        // Apply instant HP bonus from LIFE buff
        if (lifeBuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, actingUnit.getId(),
                u -> u.withHpBonus(lifeBuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Cleric Trinity skill.
     * Effect: Heal target for 3 HP, remove one debuff, apply LIFE buff (+3 HP instant).
     */
    private GameState applySkillTrinity(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int healAmount = skill.getHealAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Update units: caster uses skill, target heals
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                if (u.getId().equals(targetUnitId)) {
                    // Self-heal case
                    newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
                } else {
                    newUnits.add(u.withSkillUsed(cooldown));
                }
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Remove one random debuff from target
        Map<String, List<BuffInstance>> newUnitBuffs = removeOneRandomDebuff(state.getUnitBuffs(), targetUnitId);

        // Apply LIFE buff to target (+3 HP instant)
        BuffInstance lifeBuff = BuffFactory.create(BuffType.LIFE, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(lifeBuff);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        // Apply instant HP bonus from LIFE buff
        if (lifeBuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, targetUnitId,
                u -> u.withHpBonus(lifeBuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Cleric Power of Many skill.
     * Effect: Heal ALL friendly units for 1 HP, grant all allies +1 ATK for 1 round.
     */
    private GameState applySkillPowerOfMany(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int healAmount = skill.getHealAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        int atkBonusDuration = skill.getEffectDuration();  // 1 round

        // Find all friendly units
        List<String> friendlyUnitIds = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
                friendlyUnitIds.add(u.getId());
            }
        }

        // Update units: heal all friendlies, caster uses skill
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
            } else if (friendlyUnitIds.contains(u.getId())) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Apply +1 ATK buff to all friendlies for 1 round (no instant HP bonus)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        for (String unitId : friendlyUnitIds) {
            BuffInstance atkBuff = createAtkBuff(actingUnit.getId(), 1, atkBonusDuration);
            List<BuffInstance> unitBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(unitId, Collections.emptyList())
            );
            unitBuffs.add(atkBuff);
            newUnitBuffs.put(unitId, unitBuffs);
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    // =========================================================================
    // Phase 4C Skills
    // =========================================================================

    /**
     * Apply Warrior Heroic Leap skill.
     * Effect: Leap to any tile within range 3, deal 2 damage to all adjacent enemies on landing.
     */
    private GameState applySkillHeroicLeap(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int damage = skill.getDamageAmount();  // 2
        int cooldown = skill.getCooldown();  // 2

        // Find all enemies adjacent to landing position
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(targetPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        adjacentEnemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track damage to each unit (including guardians)
        Map<String, Integer> damageAmounts = new HashMap<>();
        for (Unit enemy : adjacentEnemies) {
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();
            damageAmounts.merge(damageReceiverId, damage, Integer::sum);
        }

        // Apply changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withPosition(targetPos));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Huntress Spectral Blades skill.
     * Effect: Deal 1 damage to all enemies in a straight line (piercing).
     */
    private GameState applySkillSpectralBlades(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        Position heroPos = actingUnit.getPosition();

        // Determine direction
        int dx = Integer.compare(targetPos.getX(), heroPos.getX());
        int dy = Integer.compare(targetPos.getY(), heroPos.getY());

        // Find all enemies in the line
        Map<String, Integer> damageAmounts = new HashMap<>();
        Position current = new Position(heroPos.getX() + dx, heroPos.getY() + dy);

        while (isInBounds(current, state.getBoard())) {
            // Check for enemies at this position
            for (Unit u : state.getUnits()) {
                if (u.isAlive() &&
                    !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                    u.getPosition().getX() == current.getX() &&
                    u.getPosition().getY() == current.getY()) {
                    // Check Guardian intercept
                    Unit guardian = findGuardian(state, u);
                    String damageReceiverId = (guardian != null) ? guardian.getId() : u.getId();
                    damageAmounts.merge(damageReceiverId, damage, Integer::sum);
                }
            }
            current = new Position(current.getX() + dx, current.getY() + dy);
        }

        // Apply changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Rogue Smoke Bomb skill.
     * Effect: Teleport to target tile, become invisible for 1 round, apply BLIND to adjacent enemies.
     */
    private GameState applySkillSmokeBomb(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2
        Position originalPos = actingUnit.getPosition();

        // Find all enemies adjacent to original position (before teleport)
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(originalPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Update caster: teleport and become invisible
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withPosition(targetPos).withInvisible(true));
            } else {
                newUnits.add(u);
            }
        }

        // Apply BLIND to adjacent enemies
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        for (Unit enemy : adjacentEnemies) {
            BuffInstance blindBuff = BuffFactory.create(BuffType.BLIND, actingUnit.getId());
            List<BuffInstance> enemyBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(enemy.getId(), Collections.emptyList())
            );
            enemyBuffs.add(blindBuff);
            newUnitBuffs.put(enemy.getId(), enemyBuffs);
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Mage Warp Beacon skill.
     * Effect: If no beacon placed, place beacon at target. If beacon exists, teleport to it.
     */
    private GameState applySkillWarpBeacon(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2

        // Check if beacon already exists
        Map<String, Object> skillState = actingUnit.getSkillState();
        boolean hasBeacon = skillState != null && skillState.containsKey("beacon_x");

        if (!hasBeacon) {
            // First use: place beacon at target position
            Map<String, Object> newSkillState = new HashMap<>();
            newSkillState.put("beacon_x", targetPos.getX());
            newSkillState.put("beacon_y", targetPos.getY());

            // Update unit with beacon placed (NO cooldown trigger)
            List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
                u -> u.withSkillStateAndActionUsed(newSkillState));

            return state.withUnits(newUnits);
        } else {
            // Second use: teleport to beacon position
            int beaconX = (Integer) skillState.get("beacon_x");
            int beaconY = (Integer) skillState.get("beacon_y");
            Position beaconPos = new Position(beaconX, beaconY);

            // Update unit: teleport to beacon, clear skill state, trigger cooldown
            List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
                u -> u.withPositionSkillStateClearedAndSkillUsed(beaconPos, cooldown));

            GameOverResult gameOver = checkGameOver(newUnits);
            return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
        }
    }

    // =========================================================================
    // Phase 4D Skills
    // =========================================================================

    /**
     * Apply Mage Wild Magic skill.
     * Effect: Deal 1 damage to ALL enemies, 33% chance to apply random debuff to each.
     */
    private GameState applySkillWildMagic(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2

        // Find all enemies
        List<Unit> enemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && !u.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
                enemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        enemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track damage to each unit (including guardians)
        Map<String, Integer> damageAmounts = new HashMap<>();
        Map<String, Unit> originalTargets = new HashMap<>();
        for (Unit enemy : enemies) {
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();
            damageAmounts.merge(damageReceiverId, damage, Integer::sum);
            originalTargets.put(enemy.getId(), enemy);
        }

        // Apply damage
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        // 33% chance to apply random debuff to each original target
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffType[] debuffs = {BuffType.WEAKNESS, BuffType.BLEED, BuffType.SLOW};
        for (Unit enemy : enemies) {
            if (rngProvider.nextInt(100) < 33) {
                BuffType debuffType = debuffs[rngProvider.nextInt(3)];
                BuffInstance debuff = BuffFactory.create(debuffType, actingUnit.getId());

                List<BuffInstance> targetBuffs = new ArrayList<>(
                    newUnitBuffs.getOrDefault(enemy.getId(), Collections.emptyList())
                );
                targetBuffs.add(debuff);
                newUnitBuffs.put(enemy.getId(), targetBuffs);

                // Apply instant HP effects for WEAKNESS (-1 HP)
                if (debuff.getInstantHpBonus() != 0) {
                    newUnits = updateUnitInList(newUnits, enemy.getId(),
                        u -> u.withHpBonus(debuff.getInstantHpBonus()));
                }
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Duelist Elemental Strike skill.
     * Effect: Deal 3 damage to target, player chooses debuff to apply (WEAKNESS, BLEED, or SLOW).
     */
    private GameState applySkillElementalStrike(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Get player's chosen debuff type (default to BLEED if not specified)
        BuffType chosenDebuff = action.getSkillChosenBuffType();
        if (chosenDebuff == null) {
            chosenDebuff = BuffType.BLEED;  // Default
        }

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // Apply chosen debuff
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance debuff = BuffFactory.create(chosenDebuff, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(damageReceiverId, Collections.emptyList())
        );
        targetBuffs.add(debuff);
        newUnitBuffs.put(damageReceiverId, targetBuffs);

        // Apply instant HP effects for WEAKNESS (-1 HP)
        if (debuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, damageReceiverId,
                u -> u.withHpBonus(debuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Rogue Death Mark skill.
     * Effect: Mark target with DEATH_MARK buff, they take +2 damage from all sources.
     */
    private GameState applySkillDeathMark(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply DEATH_MARK buff to target
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance deathMark = BuffFactory.create(BuffType.DEATH_MARK, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(deathMark);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Cleric Ascended Form skill.
     * Effect: Become invulnerable for 1 round.
     */
    private GameState applySkillAscendedForm(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsedAndInvulnerable(cooldown, true));

        // Apply INVULNERABLE buff to self
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance invulnerableBuff = BuffFactory.create(BuffType.INVULNERABLE, actingUnit.getId());
        List<BuffInstance> selfBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        selfBuffs.add(invulnerableBuff);
        newUnitBuffs.put(actingUnit.getId(), selfBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Rogue Shadow Clone skill.
     * Effect: Create a shadow clone minion at target position.
     */
    private GameState applySkillShadowClone(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2
        int cloneDuration = skill.getEffectDuration();  // 2 rounds

        // Update caster: use skill
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else {
                newUnits.add(u);
            }
        }

        // Create the Shadow Clone
        String cloneId = actingUnit.getId() + "_clone_" + System.currentTimeMillis();
        Unit clone = new Unit(
            cloneId,
            actingUnit.getOwner(),
            1,  // HP = 1
            1,  // ATK = 1
            actingUnit.getMoveRange(),  // Same move range as Rogue
            1,  // Attack range = 1 (melee)
            targetPos,
            true,  // alive
            UnitCategory.MINION,  // Category: MINION (so it doesn't end game if killed)
            MinionType.ASSASSIN,  // Type: ASSASSIN (fast minion)
            null,  // No hero class
            1,  // maxHp = 1
            null,  // No skill
            0,    // skillCooldown
            0,    // shield
            false, // invisible
            false, // invulnerable
            true,  // isTemporary (clone disappears after duration)
            cloneDuration, // temporaryDuration
            null   // skillState
        );
        newUnits.add(clone);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Duelist Feint skill.
     * Effect: Apply FEINT buff - dodge next attack and counter for 2 damage.
     */
    private GameState applySkillFeint(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply FEINT buff to self
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance feintBuff = BuffFactory.create(BuffType.FEINT, actingUnit.getId());
        List<BuffInstance> selfBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        selfBuffs.add(feintBuff);
        newUnitBuffs.put(actingUnit.getId(), selfBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Duelist Challenge skill.
     * Effect: Mark enemy with CHALLENGE buff - they must attack the Duelist or deal 50% damage.
     */
    private GameState applySkillChallenge(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply CHALLENGE buff to target (sourceUnitId tracks who challenged them)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance challengeBuff = BuffFactory.create(BuffType.CHALLENGE, actingUnit.getId());

        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(challengeBuff);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }
}
