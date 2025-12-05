package com.tactics.engine.skill;

import com.tactics.engine.model.HeroClass;

import java.util.*;

/**
 * Registry containing all skill definitions.
 * Provides lookup by skillId and by heroClass.
 */
public class SkillRegistry {

    private static final Map<String, SkillDefinition> SKILLS_BY_ID = new HashMap<>();
    private static final Map<HeroClass, List<SkillDefinition>> SKILLS_BY_CLASS = new EnumMap<>(HeroClass.class);

    // Skill ID constants
    public static final String WARRIOR_HEROIC_LEAP = "warrior_heroic_leap";
    public static final String WARRIOR_SHOCKWAVE = "warrior_shockwave";
    public static final String WARRIOR_ENDURE = "warrior_endure";

    public static final String MAGE_ELEMENTAL_BLAST = "mage_elemental_blast";
    public static final String MAGE_WARP_BEACON = "mage_warp_beacon";
    public static final String MAGE_WILD_MAGIC = "mage_wild_magic";

    public static final String ROGUE_SMOKE_BOMB = "rogue_smoke_bomb";
    public static final String ROGUE_DEATH_MARK = "rogue_death_mark";
    public static final String ROGUE_SHADOW_CLONE = "rogue_shadow_clone";

    public static final String HUNTRESS_SPIRIT_HAWK = "huntress_spirit_hawk";
    public static final String HUNTRESS_SPECTRAL_BLADES = "huntress_spectral_blades";
    public static final String HUNTRESS_NATURES_POWER = "huntress_natures_power";

    public static final String DUELIST_CHALLENGE = "duelist_challenge";
    public static final String DUELIST_ELEMENTAL_STRIKE = "duelist_elemental_strike";
    public static final String DUELIST_FEINT = "duelist_feint";

    public static final String CLERIC_TRINITY = "cleric_trinity";
    public static final String CLERIC_POWER_OF_MANY = "cleric_power_of_many";
    public static final String CLERIC_ASCENDED_FORM = "cleric_ascended_form";

    static {
        initializeSkills();
    }

    private static void initializeSkills() {
        // Initialize class lists
        for (HeroClass hc : HeroClass.values()) {
            SKILLS_BY_CLASS.put(hc, new ArrayList<>());
        }

        // =====================================================================
        // WARRIOR Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            WARRIOR_HEROIC_LEAP,
            "Heroic Leap",
            "Leap to target tile, deal 2 damage to adjacent enemies on landing",
            HeroClass.WARRIOR,
            TargetType.SINGLE_TILE,
            3, // range
            2, // cooldown
            Arrays.asList(SkillEffect.MOVE_SELF, SkillEffect.DAMAGE_AREA_ON_ARRIVAL),
            2, 0, 0, 0  // damage=2
        ));

        registerSkill(new SkillDefinition(
            WARRIOR_SHOCKWAVE,
            "Shockwave",
            "Deal 1 damage to adjacent enemies and push them 1 tile",
            HeroClass.WARRIOR,
            TargetType.AREA_AROUND_SELF,
            1, // range (adjacent)
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE, SkillEffect.KNOCKBACK),
            1, 0, 0, 0  // damage=1
        ));

        registerSkill(new SkillDefinition(
            WARRIOR_ENDURE,
            "Endure",
            "Gain 3 shield for 2 rounds, remove BLEED",
            HeroClass.WARRIOR,
            TargetType.SELF,
            0, // no range needed
            2, // cooldown
            Arrays.asList(SkillEffect.APPLY_SHIELD, SkillEffect.REMOVE_BUFF),
            0, 0, 3, 2  // shield=3, duration=2
        ));

        // =====================================================================
        // MAGE Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            MAGE_ELEMENTAL_BLAST,
            "Elemental Blast",
            "Deal 3 damage, 50% chance to apply random debuff",
            HeroClass.MAGE,
            TargetType.SINGLE_ENEMY,
            3, // range
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE, SkillEffect.APPLY_BUFF),
            3, 0, 0, 0  // damage=3
        ));

        registerSkill(new SkillDefinition(
            MAGE_WARP_BEACON,
            "Warp Beacon",
            "Place beacon or teleport to existing beacon",
            HeroClass.MAGE,
            TargetType.SINGLE_TILE,
            4, // range
            2, // cooldown
            Arrays.asList(SkillEffect.MOVE_SELF),
            0, 0, 0, 0
        ));

        registerSkill(new SkillDefinition(
            MAGE_WILD_MAGIC,
            "Wild Magic",
            "Deal 1 damage to ALL enemies, 33% chance each to apply debuff",
            HeroClass.MAGE,
            TargetType.ALL_ENEMIES,
            0, // no range (hits all)
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE, SkillEffect.APPLY_BUFF),
            1, 0, 0, 0  // damage=1
        ));

        // =====================================================================
        // ROGUE Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            ROGUE_SMOKE_BOMB,
            "Smoke Bomb",
            "Teleport to tile, become invisible 1 round, blind adjacent enemies",
            HeroClass.ROGUE,
            TargetType.SINGLE_TILE,
            3, // range
            2, // cooldown
            Arrays.asList(SkillEffect.MOVE_SELF, SkillEffect.INVISIBILITY, SkillEffect.APPLY_BUFF),
            0, 0, 0, 1  // duration=1
        ));

        registerSkill(new SkillDefinition(
            ROGUE_DEATH_MARK,
            "Death Mark",
            "Mark enemy for 2 rounds: +2 damage taken, heal 2 if killed",
            HeroClass.ROGUE,
            TargetType.SINGLE_ENEMY,
            2, // range
            2, // cooldown
            Arrays.asList(SkillEffect.MARK),
            2, 2, 0, 2  // damage bonus=2, heal=2, duration=2
        ));

        registerSkill(new SkillDefinition(
            ROGUE_SHADOW_CLONE,
            "Shadow Clone",
            "Summon 1HP/1ATK clone on adjacent tile for 2 rounds",
            HeroClass.ROGUE,
            TargetType.SINGLE_TILE,
            1, // range (adjacent only)
            2, // cooldown
            Arrays.asList(SkillEffect.SPAWN_UNIT),
            0, 0, 0, 2  // duration=2
        ));

        // =====================================================================
        // HUNTRESS Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            HUNTRESS_SPIRIT_HAWK,
            "Spirit Hawk",
            "Deal 2 damage to enemy at long range",
            HeroClass.HUNTRESS,
            TargetType.SINGLE_ENEMY,
            4, // range
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE),
            2, 0, 0, 0  // damage=2
        ));

        registerSkill(new SkillDefinition(
            HUNTRESS_SPECTRAL_BLADES,
            "Spectral Blades",
            "Deal 1 damage to all enemies in a line",
            HeroClass.HUNTRESS,
            TargetType.LINE,
            3, // range
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE),
            1, 0, 0, 0  // damage=1
        ));

        registerSkill(new SkillDefinition(
            HUNTRESS_NATURES_POWER,
            "Nature's Power",
            "Next 2 attacks deal +2 damage, gain LIFE buff",
            HeroClass.HUNTRESS,
            TargetType.SELF,
            0, // no range
            2, // cooldown
            Arrays.asList(SkillEffect.EMPOWER_ATTACKS, SkillEffect.APPLY_BUFF),
            2, 0, 0, 2  // damage bonus=2, effect count=2
        ));

        // =====================================================================
        // DUELIST Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            DUELIST_CHALLENGE,
            "Challenge",
            "Challenge enemy for 2 rounds: counter-attack for 2 damage when attacked",
            HeroClass.DUELIST,
            TargetType.SINGLE_ENEMY,
            2, // range
            2, // cooldown
            Arrays.asList(SkillEffect.MARK),
            2, 0, 0, 2  // counter damage=2, duration=2
        ));

        registerSkill(new SkillDefinition(
            DUELIST_ELEMENTAL_STRIKE,
            "Elemental Strike",
            "Deal 3 damage to adjacent enemy, apply chosen debuff",
            HeroClass.DUELIST,
            TargetType.SINGLE_ENEMY,
            1, // range (adjacent)
            2, // cooldown
            Arrays.asList(SkillEffect.DAMAGE, SkillEffect.APPLY_BUFF),
            3, 0, 0, 0  // damage=3
        ));

        registerSkill(new SkillDefinition(
            DUELIST_FEINT,
            "Feint",
            "Dodge next attack and counter for 2 damage",
            HeroClass.DUELIST,
            TargetType.SELF,
            0, // no range
            2, // cooldown
            Arrays.asList(SkillEffect.MARK),  // Uses mark system for tracking
            2, 0, 0, 2  // counter damage=2, duration=2
        ));

        // =====================================================================
        // CLERIC Skills
        // =====================================================================

        registerSkill(new SkillDefinition(
            CLERIC_TRINITY,
            "Trinity",
            "Heal 3 HP, remove debuffs, apply LIFE buff",
            HeroClass.CLERIC,
            TargetType.SINGLE_ALLY,
            2, // range
            2, // cooldown
            Arrays.asList(SkillEffect.HEAL, SkillEffect.REMOVE_BUFF, SkillEffect.APPLY_BUFF),
            0, 3, 0, 0  // heal=3
        ));

        registerSkill(new SkillDefinition(
            CLERIC_POWER_OF_MANY,
            "Power of Many",
            "Heal all allies 1 HP, all allies +1 ATK for 1 round",
            HeroClass.CLERIC,
            TargetType.ALL_ALLIES,
            0, // no range (hits all)
            2, // cooldown
            Arrays.asList(SkillEffect.HEAL, SkillEffect.APPLY_BUFF),
            0, 1, 0, 1  // heal=1, duration=1
        ));

        registerSkill(new SkillDefinition(
            CLERIC_ASCENDED_FORM,
            "Ascended Form",
            "Become invulnerable for 1 round, double healing, cannot attack",
            HeroClass.CLERIC,
            TargetType.SELF,
            0, // no range
            2, // cooldown
            Arrays.asList(SkillEffect.INVULNERABILITY),
            0, 0, 0, 1  // duration=1
        ));
    }

    private static void registerSkill(SkillDefinition skill) {
        SKILLS_BY_ID.put(skill.getSkillId(), skill);
        SKILLS_BY_CLASS.get(skill.getHeroClass()).add(skill);
    }

    /**
     * Get skill definition by ID.
     * @return SkillDefinition or null if not found
     */
    public static SkillDefinition getSkill(String skillId) {
        return SKILLS_BY_ID.get(skillId);
    }

    /**
     * Get all skills for a hero class.
     * @return Unmodifiable list of skills for the class
     */
    public static List<SkillDefinition> getSkillsForClass(HeroClass heroClass) {
        List<SkillDefinition> skills = SKILLS_BY_CLASS.get(heroClass);
        return skills != null ? Collections.unmodifiableList(skills) : Collections.emptyList();
    }

    /**
     * Get all registered skill IDs.
     */
    public static Set<String> getAllSkillIds() {
        return Collections.unmodifiableSet(SKILLS_BY_ID.keySet());
    }

    /**
     * Check if a skill ID is valid.
     */
    public static boolean isValidSkillId(String skillId) {
        return SKILLS_BY_ID.containsKey(skillId);
    }

    /**
     * Check if a hero class can use a specific skill.
     */
    public static boolean canClassUseSkill(HeroClass heroClass, String skillId) {
        SkillDefinition skill = SKILLS_BY_ID.get(skillId);
        return skill != null && skill.getHeroClass() == heroClass;
    }
}
