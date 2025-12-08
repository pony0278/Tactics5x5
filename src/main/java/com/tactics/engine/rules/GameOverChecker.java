package com.tactics.engine.rules;

import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles game over and death checking logic.
 * Extracted from ActionExecutor for better code organization.
 */
public class GameOverChecker {

    /**
     * Result of a game over check.
     */
    public static class GameOverResult {
        private final boolean isGameOver;
        private final PlayerId winner;

        public GameOverResult(boolean isGameOver, PlayerId winner) {
            this.isGameOver = isGameOver;
            this.winner = winner;
        }

        public boolean isGameOver() {
            return isGameOver;
        }

        public PlayerId getWinner() {
            return winner;
        }
    }

    /**
     * Check if the game is over (without active player info for ties).
     */
    public GameOverResult checkGameOver(List<Unit> units) {
        return checkGameOver(units, null);
    }

    /**
     * Check if the game is over.
     *
     * V3 Victory Condition: Kill the enemy Hero to win.
     * Minion deaths do not end the game.
     * For backward compatibility: if no Heroes exist, fall back to any unit check.
     *
     * @param units List of units
     * @param activePlayer Active player (for tie-breaking in simultaneous death)
     * @return GameOverResult with isGameOver and winner
     */
    public GameOverResult checkGameOver(List<Unit> units, PlayerId activePlayer) {
        boolean p1HeroAlive = false;
        boolean p2HeroAlive = false;
        boolean p1HasHero = false;
        boolean p2HasHero = false;
        boolean p1HasAlive = false;
        boolean p2HasAlive = false;

        for (Unit u : units) {
            if (u.isHero()) {
                if (u.getOwner().isPlayer1()) {
                    p1HasHero = true;
                    if (u.isAlive()) {
                        p1HeroAlive = true;
                    }
                } else {
                    p2HasHero = true;
                    if (u.isAlive()) {
                        p2HeroAlive = true;
                    }
                }
            }
            if (u.isAlive()) {
                if (u.getOwner().isPlayer1()) {
                    p1HasAlive = true;
                } else {
                    p2HasAlive = true;
                }
            }
        }

        // V3 mode: Both players have Heroes - check Hero alive status
        if (p1HasHero && p2HasHero) {
            // Simultaneous Hero death: Active player wins
            if (!p1HeroAlive && !p2HeroAlive) {
                if (activePlayer != null) {
                    return new GameOverResult(true, activePlayer);
                }
                return new GameOverResult(true, PlayerId.PLAYER_1);
            }

            // P1 Hero dead: P2 wins
            if (!p1HeroAlive) {
                return new GameOverResult(true, PlayerId.PLAYER_2);
            }
            // P2 Hero dead: P1 wins
            if (!p2HeroAlive) {
                return new GameOverResult(true, PlayerId.PLAYER_1);
            }
            return new GameOverResult(false, null);
        }

        // Legacy/backward compatibility: No Heroes - check any unit alive status
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
     * Check if any minion died in the units list (HP <= 0 but was created alive).
     * Returns a DeathChoice for the first dead minion found (by ID order for determinism).
     * Only checks minions, not heroes (hero death ends the game, no death choice).
     * Excludes temporary units (like Shadow Clone) - they don't trigger death choice.
     *
     * @param units The current units list
     * @param originalUnits The units list before the action (to compare alive status)
     * @return DeathChoice if a minion died, null otherwise
     */
    public DeathChoice checkMinionDeath(List<Unit> units, List<Unit> originalUnits) {
        // Build map of original alive minions (excluding temporary units)
        Map<String, Unit> originalMinionMap = new HashMap<>();
        for (Unit u : originalUnits) {
            if (u.isAlive() && u.getCategory() == UnitCategory.MINION && !u.isTemporary()) {
                originalMinionMap.put(u.getId(), u);
            }
        }

        // Find first minion that died (sort by ID for determinism), excluding temporary units
        List<Unit> deadMinions = new ArrayList<>();
        for (Unit u : units) {
            if (!u.isAlive() && u.getCategory() == UnitCategory.MINION
                && !u.isTemporary() && originalMinionMap.containsKey(u.getId())) {
                deadMinions.add(u);
            }
        }

        if (deadMinions.isEmpty()) {
            return null;
        }

        // Sort by ID for deterministic order
        deadMinions.sort((a, b) -> a.getId().compareTo(b.getId()));
        Unit firstDead = deadMinions.get(0);

        return new DeathChoice(
            firstDead.getId(),
            firstDead.getOwner(),
            firstDead.getPosition()
        );
    }
}
