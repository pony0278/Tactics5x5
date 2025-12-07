package com.tactics.engine.draft;

import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.skill.SkillDefinition;
import com.tactics.engine.skill.SkillRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents the draft selection state for a single player.
 *
 * Immutable class - all modifications return new instances.
 *
 * Draft consists of:
 * - Hero class (provided, determines available skills)
 * - 2 minion selections from TANK, ARCHER, ASSASSIN
 * - 1 skill selection from 3 class-specific skills
 */
public class DraftState {

    private static final int REQUIRED_MINIONS = 2;
    private static final List<MinionType> AVAILABLE_MINION_TYPES =
        Collections.unmodifiableList(Arrays.asList(MinionType.values()));

    private final PlayerId playerId;
    private final HeroClass heroClass;
    private final List<MinionType> selectedMinions;
    private final String selectedSkillId;

    /**
     * Create a new empty DraftState for a player.
     *
     * @param playerId The player's ID
     * @param heroClass The player's hero class (determines available skills)
     */
    public DraftState(PlayerId playerId, HeroClass heroClass) {
        this(playerId, heroClass, Collections.emptyList(), null);
    }

    /**
     * Private constructor for immutable updates.
     */
    private DraftState(PlayerId playerId, HeroClass heroClass,
                       List<MinionType> selectedMinions, String selectedSkillId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.heroClass = Objects.requireNonNull(heroClass, "heroClass cannot be null");
        this.selectedMinions = Collections.unmodifiableList(new ArrayList<>(selectedMinions));
        this.selectedSkillId = selectedSkillId;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    public PlayerId getPlayerId() {
        return playerId;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    /**
     * Get the list of selected minions (0, 1, or 2).
     * The returned list is unmodifiable.
     */
    public List<MinionType> getSelectedMinions() {
        return selectedMinions;
    }

    /**
     * Get the selected skill ID, or null if not yet selected.
     */
    public String getSelectedSkillId() {
        return selectedSkillId;
    }

    // =========================================================================
    // Completion Checks
    // =========================================================================

    /**
     * Check if minion selection is complete (2 minions selected).
     */
    public boolean isMinionSelectionComplete() {
        return selectedMinions.size() >= REQUIRED_MINIONS;
    }

    /**
     * Check if skill selection is complete (1 skill selected).
     */
    public boolean isSkillSelectionComplete() {
        return selectedSkillId != null;
    }

    /**
     * Check if this player's draft is fully complete.
     */
    public boolean isComplete() {
        return isMinionSelectionComplete() && isSkillSelectionComplete();
    }

    // =========================================================================
    // Available Options
    // =========================================================================

    /**
     * Get the list of available minion types.
     * All 3 types are always available (can select same type twice).
     */
    public static List<MinionType> getAvailableMinionTypes() {
        return AVAILABLE_MINION_TYPES;
    }

    /**
     * Get the list of available skill IDs for this player's hero class.
     */
    public List<String> getAvailableSkillIds() {
        List<SkillDefinition> skills = SkillRegistry.getSkillsForClass(heroClass);
        return skills.stream()
            .map(SkillDefinition::getSkillId)
            .collect(Collectors.toList());
    }

    // =========================================================================
    // Immutable Updates
    // =========================================================================

    /**
     * Add a minion selection, returning a new DraftState.
     *
     * @param minionType The minion type to add
     * @return New DraftState with the minion added
     * @throws IllegalArgumentException if minionType is null
     * @throws IllegalStateException if 2 minions already selected
     */
    public DraftState withMinion(MinionType minionType) {
        if (minionType == null) {
            throw new IllegalArgumentException("Minion type cannot be null");
        }
        if (isMinionSelectionComplete()) {
            throw new IllegalStateException("Cannot add more minions - 2 already selected");
        }

        List<MinionType> newMinions = new ArrayList<>(selectedMinions);
        newMinions.add(minionType);

        return new DraftState(playerId, heroClass, newMinions, selectedSkillId);
    }

    /**
     * Set the skill selection, returning a new DraftState.
     *
     * @param skillId The skill ID to select
     * @return New DraftState with the skill selected
     * @throws IllegalArgumentException if skillId is null, invalid, or wrong class
     * @throws IllegalStateException if skill already selected
     */
    public DraftState withSkill(String skillId) {
        if (skillId == null) {
            throw new IllegalArgumentException("Skill ID cannot be null");
        }
        if (isSkillSelectionComplete()) {
            throw new IllegalStateException("Cannot change skill - already selected");
        }
        if (!SkillRegistry.isValidSkillId(skillId)) {
            throw new IllegalArgumentException("Invalid skill ID: " + skillId);
        }
        if (!SkillRegistry.canClassUseSkill(heroClass, skillId)) {
            throw new IllegalArgumentException(
                "Skill " + skillId + " cannot be used by " + heroClass);
        }

        return new DraftState(playerId, heroClass, selectedMinions, skillId);
    }

    // =========================================================================
    // Object Methods
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DraftState that = (DraftState) o;
        return Objects.equals(playerId, that.playerId) &&
               heroClass == that.heroClass &&
               Objects.equals(selectedMinions, that.selectedMinions) &&
               Objects.equals(selectedSkillId, that.selectedSkillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, heroClass, selectedMinions, selectedSkillId);
    }

    @Override
    public String toString() {
        return "DraftState{" +
               "playerId=" + playerId +
               ", heroClass=" + heroClass +
               ", selectedMinions=" + selectedMinions +
               ", selectedSkillId='" + selectedSkillId + '\'' +
               ", complete=" + isComplete() +
               '}';
    }
}
