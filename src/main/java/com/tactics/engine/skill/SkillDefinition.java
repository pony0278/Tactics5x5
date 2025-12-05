package com.tactics.engine.skill;

import com.tactics.engine.model.HeroClass;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a hero skill.
 * Contains all static data about a skill (not instance state like cooldown).
 */
public class SkillDefinition {

    private final String skillId;
    private final String name;
    private final String description;
    private final HeroClass heroClass;
    private final TargetType targetType;
    private final int range;
    private final int cooldown;
    private final List<SkillEffect> effects;

    // Effect parameters (used by specific effects)
    private final int damageAmount;
    private final int healAmount;
    private final int shieldAmount;
    private final int effectDuration;

    /**
     * Full constructor for skill definition.
     */
    public SkillDefinition(String skillId, String name, String description, HeroClass heroClass,
                           TargetType targetType, int range, int cooldown, List<SkillEffect> effects,
                           int damageAmount, int healAmount, int shieldAmount, int effectDuration) {
        this.skillId = skillId;
        this.name = name;
        this.description = description;
        this.heroClass = heroClass;
        this.targetType = targetType;
        this.range = range;
        this.cooldown = cooldown;
        this.effects = effects != null ? Collections.unmodifiableList(effects) : Collections.emptyList();
        this.damageAmount = damageAmount;
        this.healAmount = healAmount;
        this.shieldAmount = shieldAmount;
        this.effectDuration = effectDuration;
    }

    /**
     * Simplified constructor for common cases.
     */
    public SkillDefinition(String skillId, String name, String description, HeroClass heroClass,
                           TargetType targetType, int range, int cooldown, List<SkillEffect> effects) {
        this(skillId, name, description, heroClass, targetType, range, cooldown, effects, 0, 0, 0, 0);
    }

    // Getters

    public String getSkillId() {
        return skillId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public int getRange() {
        return range;
    }

    public int getCooldown() {
        return cooldown;
    }

    public List<SkillEffect> getEffects() {
        return effects;
    }

    public int getDamageAmount() {
        return damageAmount;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public int getShieldAmount() {
        return shieldAmount;
    }

    public int getEffectDuration() {
        return effectDuration;
    }

    // Helper methods

    /**
     * Check if this skill requires a target position.
     */
    public boolean requiresTargetPosition() {
        return targetType == TargetType.SINGLE_ENEMY ||
               targetType == TargetType.SINGLE_ALLY ||
               targetType == TargetType.SINGLE_TILE ||
               targetType == TargetType.LINE ||
               targetType == TargetType.AREA_AROUND_TARGET;
    }

    /**
     * Check if this skill requires a target unit.
     */
    public boolean requiresTargetUnit() {
        return targetType == TargetType.SINGLE_ENEMY ||
               targetType == TargetType.SINGLE_ALLY;
    }

    /**
     * Check if this skill targets enemies.
     */
    public boolean targetsEnemies() {
        return targetType == TargetType.SINGLE_ENEMY ||
               targetType == TargetType.ALL_ENEMIES;
    }

    /**
     * Check if this skill targets allies.
     */
    public boolean targetsAllies() {
        return targetType == TargetType.SINGLE_ALLY ||
               targetType == TargetType.ALL_ALLIES;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillDefinition that = (SkillDefinition) o;
        return Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillId);
    }

    @Override
    public String toString() {
        return "SkillDefinition{" +
               "skillId='" + skillId + '\'' +
               ", name='" + name + '\'' +
               ", heroClass=" + heroClass +
               ", targetType=" + targetType +
               ", range=" + range +
               ", cooldown=" + cooldown +
               '}';
    }
}
