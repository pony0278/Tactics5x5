package com.tactics.engine.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a unit on the board.
 * V3 extends this with category, hero/minion types, skill-related fields,
 * and action state (for SPEED/SLOW buffs).
 */
public class Unit {

    // Core fields (V1/V2)
    private final String id;
    private final PlayerId owner;
    private final int hp;
    private final int attack;
    private final int moveRange;
    private final int attackRange;
    private final Position position;
    private final boolean alive;

    // V3 Unit Category fields
    private final UnitCategory category;      // HERO or MINION
    private final MinionType minionType;      // null if HERO
    private final HeroClass heroClass;        // null if MINION
    private final int maxHp;                  // Maximum HP for this unit

    // V3 Hero Skill fields
    private final String selectedSkillId;     // Hero only - selected skill
    private final int skillCooldown;          // Hero only - rounds until skill ready (0 = ready)

    // V3 Skill State fields
    private final int shield;                 // Shield points (e.g., from Endure skill)
    private final boolean invisible;          // Smoke Bomb effect
    private final boolean invulnerable;       // Ascended Form effect
    private final boolean isTemporary;        // Shadow Clone - temporary unit
    private final int temporaryDuration;      // Rounds remaining for temporary unit
    private final Map<String, Object> skillState;  // Skill-specific state (e.g., Warp Beacon position)

    // V3 Action State fields (for SPEED/SLOW buffs)
    private final int actionsUsed;            // Number of actions used this round (0, 1, or 2 with SPEED)
    private final boolean preparing;          // SLOW buff: unit is preparing an action
    private final Map<String, Object> preparingAction;  // SLOW buff: the action being prepared (serialized)

    // V3 Phase 4B: Bonus Attack fields (for Nature's Power skill)
    private final int bonusAttackDamage;      // Extra damage per attack (e.g., +2 from Nature's Power)
    private final int bonusAttackCharges;     // Number of attacks with bonus damage remaining

    /**
     * V1/V2 backward-compatible constructor.
     * Creates a unit with default V3 fields (category = null, treated as legacy unit).
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive) {
        this(id, owner, hp, attack, moveRange, attackRange, position, alive,
             null, null, null, hp,
             null, 0,
             0, false, false, false, 0, null,
             0, false, null,
             0, 0);
    }

    /**
     * V3 constructor without action state (for backward compatibility).
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive,
                UnitCategory category, MinionType minionType, HeroClass heroClass, int maxHp,
                String selectedSkillId, int skillCooldown,
                int shield, boolean invisible, boolean invulnerable, boolean isTemporary, int temporaryDuration,
                Map<String, Object> skillState) {
        this(id, owner, hp, attack, moveRange, attackRange, position, alive,
             category, minionType, heroClass, maxHp,
             selectedSkillId, skillCooldown,
             shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
             0, false, null,
             0, 0);
    }

    /**
     * V3 constructor with action state (for backward compatibility).
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive,
                UnitCategory category, MinionType minionType, HeroClass heroClass, int maxHp,
                String selectedSkillId, int skillCooldown,
                int shield, boolean invisible, boolean invulnerable, boolean isTemporary, int temporaryDuration,
                Map<String, Object> skillState,
                int actionsUsed, boolean preparing, Map<String, Object> preparingAction) {
        this(id, owner, hp, attack, moveRange, attackRange, position, alive,
             category, minionType, heroClass, maxHp,
             selectedSkillId, skillCooldown,
             shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
             actionsUsed, preparing, preparingAction,
             0, 0);
    }

    /**
     * V3 full constructor with all fields including action state and bonus attack.
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive,
                UnitCategory category, MinionType minionType, HeroClass heroClass, int maxHp,
                String selectedSkillId, int skillCooldown,
                int shield, boolean invisible, boolean invulnerable, boolean isTemporary, int temporaryDuration,
                Map<String, Object> skillState,
                int actionsUsed, boolean preparing, Map<String, Object> preparingAction,
                int bonusAttackDamage, int bonusAttackCharges) {
        this.id = id;
        this.owner = owner;
        this.hp = hp;
        this.attack = attack;
        this.moveRange = moveRange;
        this.attackRange = attackRange;
        this.position = position;
        this.alive = alive;
        this.category = category;
        this.minionType = minionType;
        this.heroClass = heroClass;
        this.maxHp = maxHp;
        this.selectedSkillId = selectedSkillId;
        this.skillCooldown = skillCooldown;
        this.shield = shield;
        this.invisible = invisible;
        this.invulnerable = invulnerable;
        this.isTemporary = isTemporary;
        this.temporaryDuration = temporaryDuration;
        this.skillState = skillState != null ? Collections.unmodifiableMap(skillState) : Collections.emptyMap();
        this.actionsUsed = actionsUsed;
        this.preparing = preparing;
        this.preparingAction = preparingAction != null ? Collections.unmodifiableMap(preparingAction) : null;
        this.bonusAttackDamage = bonusAttackDamage;
        this.bonusAttackCharges = bonusAttackCharges;
    }

    // Core getters (V1/V2)

    public String getId() {
        return id;
    }

    public PlayerId getOwner() {
        return owner;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public int getMoveRange() {
        return moveRange;
    }

    public int getAttackRange() {
        return attackRange;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isAlive() {
        return alive;
    }

    // V3 Category getters

    public UnitCategory getCategory() {
        return category;
    }

    public MinionType getMinionType() {
        return minionType;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public int getMaxHp() {
        return maxHp;
    }

    // V3 Hero Skill getters

    public String getSelectedSkillId() {
        return selectedSkillId;
    }

    public int getSkillCooldown() {
        return skillCooldown;
    }

    // V3 Skill State getters

    public int getShield() {
        return shield;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public int getTemporaryDuration() {
        return temporaryDuration;
    }

    public Map<String, Object> getSkillState() {
        return skillState;
    }

    // V3 Action State getters

    public int getActionsUsed() {
        return actionsUsed;
    }

    public boolean isPreparing() {
        return preparing;
    }

    public Map<String, Object> getPreparingAction() {
        return preparingAction;
    }

    // V3 Phase 4B: Bonus Attack getters

    public int getBonusAttackDamage() {
        return bonusAttackDamage;
    }

    public int getBonusAttackCharges() {
        return bonusAttackCharges;
    }

    // Helper methods

    public boolean isHero() {
        return category == UnitCategory.HERO;
    }

    public boolean isMinion() {
        return category == UnitCategory.MINION;
    }

    /**
     * Check if unit has acted this round (for normal units, 1 action; for SPEED buffed, 2 actions).
     */
    public boolean hasActed() {
        return actionsUsed > 0;
    }

    /**
     * Get remaining actions for this round.
     * Normal units have 1 action, SPEED buffed units have 2.
     * This needs to be calculated based on buffs (done in RuleEngine).
     */
    public int getRemainingActions(int maxActions) {
        return Math.max(0, maxActions - actionsUsed);
    }

    // =========================================================================
    // Immutable "with" methods for creating modified copies
    // =========================================================================

    /**
     * Create a copy with updated position.
     */
    public Unit withPosition(Position newPosition) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, newPosition, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated HP and alive status.
     * V3 Phase 4C: Clears skillState (beacon) when unit dies.
     */
    public Unit withHp(int newHp) {
        boolean newAlive = newHp > 0;
        // Clear skillState when unit dies (e.g., Warp Beacon disappears on Mage death)
        Map<String, Object> newSkillState = newAlive ? skillState : null;
        return new Unit(id, owner, newHp, attack, moveRange, attackRange, position, newAlive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, newSkillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with damage applied.
     * Shield absorbs damage first, then excess damage reduces HP.
     */
    public Unit withDamage(int damage) {
        if (shield > 0) {
            int shieldAbsorbed = Math.min(shield, damage);
            int remainingDamage = damage - shieldAbsorbed;
            int newShield = shield - shieldAbsorbed;
            if (remainingDamage > 0) {
                return withShield(newShield).withHp(hp - remainingDamage);
            } else {
                return withShield(newShield);
            }
        }
        return withHp(hp - damage);
    }

    /**
     * Create a copy with HP bonus applied (healing or buff).
     */
    public Unit withHpBonus(int bonus) {
        return withHp(hp + bonus);
    }

    /**
     * Create a copy with updated actionsUsed.
     */
    public Unit withActionsUsed(int newActionsUsed) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        newActionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with incremented actionsUsed.
     */
    public Unit withActionUsed() {
        return withActionsUsed(actionsUsed + 1);
    }

    /**
     * Create a copy with updated position and incremented actionsUsed (common for MOVE).
     */
    public Unit withPositionAndActionUsed(Position newPosition) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, newPosition, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with preparing state set.
     */
    public Unit withPreparing(boolean newPreparing, Map<String, Object> newPreparingAction) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, newPreparing, newPreparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with preparing state set and actionsUsed incremented (for SLOW buff).
     */
    public Unit withPreparingAndActionUsed(Map<String, Object> newPreparingAction) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, true, newPreparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with reset action state (for round end).
     */
    public Unit withResetActionState() {
        if (actionsUsed == 0 && !preparing) {
            return this;  // No change needed
        }
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        0, false, null,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated skill cooldown.
     */
    public Unit withSkillCooldown(int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with cooldown set and actionsUsed incremented (for skill use).
     */
    public Unit withSkillUsed(int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated shield value.
     */
    public Unit withShield(int newShield) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        newShield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with shield added (stacks with existing shield).
     */
    public Unit withShieldAdded(int additionalShield) {
        return withShield(shield + additionalShield);
    }

    /**
     * Create a copy with shield and cooldown set, actionsUsed incremented (for Endure skill).
     */
    public Unit withShieldAndSkillUsed(int newShield, int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        newShield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with cooldown set, bonus attack set, and actionsUsed incremented (for Nature's Power).
     */
    public Unit withSkillUsedAndBonusAttack(int newCooldown, int newBonusDamage, int newBonusCharges) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        newBonusDamage, newBonusCharges);
    }

    /**
     * Create a copy with bonus attack consumed (after an attack with bonus damage).
     */
    public Unit withBonusAttackConsumed() {
        int newCharges = Math.max(0, bonusAttackCharges - 1);
        int newBonusDamage = newCharges > 0 ? bonusAttackDamage : 0;
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        newBonusDamage, newCharges);
    }

    /**
     * Create a copy with healing applied (increases HP, capped at maxHp).
     */
    public Unit withHealing(int healAmount) {
        int newHp = Math.min(hp + healAmount, maxHp);
        return withHp(newHp);
    }

    /**
     * Create a copy with updated invisible state.
     */
    public Unit withInvisible(boolean newInvisible) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, newInvisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated skill state map.
     */
    public Unit withSkillState(Map<String, Object> newSkillState) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, newSkillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated temporary duration.
     * Used for temporary units like Shadow Clone.
     */
    public Unit withTemporaryDuration(int newTemporaryDuration) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, newTemporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with position updated, skill used (cooldown set, actionsUsed incremented).
     * Used for movement skills like Heroic Leap, Smoke Bomb, Warp Beacon teleport.
     */
    public Unit withPositionAndSkillUsed(Position newPosition, int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, newPosition, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with position updated, invisible set, skill used.
     * Used for Smoke Bomb skill.
     */
    public Unit withPositionInvisibleAndSkillUsed(Position newPosition, boolean newInvisible, int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, newPosition, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, newInvisible, invulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with skill state updated and action used (no cooldown change).
     * Used for Warp Beacon place action.
     */
    public Unit withSkillStateAndActionUsed(Map<String, Object> newSkillState) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, newSkillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with position updated, skill state cleared, skill used.
     * Used for Warp Beacon teleport action.
     */
    public Unit withPositionSkillStateClearedAndSkillUsed(Position newPosition, int newCooldown) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, newPosition, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, invulnerable, isTemporary, temporaryDuration, null,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with skill used and invulnerable state set.
     * Used for Ascended Form skill.
     */
    public Unit withSkillUsedAndInvulnerable(int newCooldown, boolean newInvulnerable) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, invisible, newInvulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed + 1, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with updated invulnerable state.
     */
    public Unit withInvulnerable(boolean newInvulnerable) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, skillCooldown,
                        shield, invisible, newInvulnerable, isTemporary, temporaryDuration, skillState,
                        actionsUsed, preparing, preparingAction,
                        bonusAttackDamage, bonusAttackCharges);
    }

    /**
     * Create a copy with reset action state and decremented cooldown (for round end).
     * Also clears invisible and invulnerable status (they last 1 round).
     */
    public Unit withRoundEndReset() {
        int newCooldown = Math.max(0, skillCooldown - 1);
        // V3 Phase 4C: Invisible expires at round end (lasts 1 round)
        // Phase 4D: Invulnerable expires at round end (lasts 1 round)
        boolean needsChange = actionsUsed > 0 || preparing || skillCooldown > 0 || invisible || invulnerable;
        if (!needsChange) {
            return this;  // No change needed
        }
        return new Unit(id, owner, hp, attack, moveRange, attackRange, position, alive,
                        category, minionType, heroClass, maxHp,
                        selectedSkillId, newCooldown,
                        shield, false, false, isTemporary, temporaryDuration, skillState,
                        0, false, null,
                        bonusAttackDamage, bonusAttackCharges);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Unit unit = (Unit) o;
        return hp == unit.hp &&
               attack == unit.attack &&
               moveRange == unit.moveRange &&
               attackRange == unit.attackRange &&
               alive == unit.alive &&
               maxHp == unit.maxHp &&
               skillCooldown == unit.skillCooldown &&
               shield == unit.shield &&
               invisible == unit.invisible &&
               invulnerable == unit.invulnerable &&
               isTemporary == unit.isTemporary &&
               temporaryDuration == unit.temporaryDuration &&
               actionsUsed == unit.actionsUsed &&
               preparing == unit.preparing &&
               bonusAttackDamage == unit.bonusAttackDamage &&
               bonusAttackCharges == unit.bonusAttackCharges &&
               Objects.equals(id, unit.id) &&
               Objects.equals(owner, unit.owner) &&
               Objects.equals(position, unit.position) &&
               category == unit.category &&
               minionType == unit.minionType &&
               heroClass == unit.heroClass &&
               Objects.equals(selectedSkillId, unit.selectedSkillId) &&
               Objects.equals(skillState, unit.skillState) &&
               Objects.equals(preparingAction, unit.preparingAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, owner, hp, attack, moveRange, attackRange, position, alive,
                           category, minionType, heroClass, maxHp, selectedSkillId, skillCooldown,
                           shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
                           actionsUsed, preparing, preparingAction,
                           bonusAttackDamage, bonusAttackCharges);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unit{id='").append(id).append("'");
        sb.append(", owner=").append(owner);
        sb.append(", hp=").append(hp);
        sb.append(", attack=").append(attack);
        sb.append(", moveRange=").append(moveRange);
        sb.append(", attackRange=").append(attackRange);
        sb.append(", position=").append(position);
        sb.append(", alive=").append(alive);
        if (category != null) {
            sb.append(", category=").append(category);
            if (minionType != null) {
                sb.append(", minionType=").append(minionType);
            }
            if (heroClass != null) {
                sb.append(", heroClass=").append(heroClass);
            }
        }
        if (selectedSkillId != null) {
            sb.append(", selectedSkillId='").append(selectedSkillId).append("'");
            sb.append(", skillCooldown=").append(skillCooldown);
        }
        if (shield > 0) {
            sb.append(", shield=").append(shield);
        }
        if (invisible) {
            sb.append(", invisible=true");
        }
        if (invulnerable) {
            sb.append(", invulnerable=true");
        }
        if (isTemporary) {
            sb.append(", isTemporary=true, temporaryDuration=").append(temporaryDuration);
        }
        if (actionsUsed > 0) {
            sb.append(", actionsUsed=").append(actionsUsed);
        }
        if (preparing) {
            sb.append(", preparing=true");
        }
        sb.append("}");
        return sb.toString();
    }
}
