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

    /**
     * V1/V2 backward-compatible constructor.
     * Creates a unit with default V3 fields (category = null, treated as legacy unit).
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive) {
        this(id, owner, hp, attack, moveRange, attackRange, position, alive,
             null, null, null, hp,
             null, 0,
             0, false, false, false, 0, null,
             0, false, null);
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
             0, false, null);
    }

    /**
     * V3 full constructor with all fields including action state.
     */
    public Unit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position position, boolean alive,
                UnitCategory category, MinionType minionType, HeroClass heroClass, int maxHp,
                String selectedSkillId, int skillCooldown,
                int shield, boolean invisible, boolean invulnerable, boolean isTemporary, int temporaryDuration,
                Map<String, Object> skillState,
                int actionsUsed, boolean preparing, Map<String, Object> preparingAction) {
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
                           actionsUsed, preparing, preparingAction);
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
