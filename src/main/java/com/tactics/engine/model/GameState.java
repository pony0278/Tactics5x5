package com.tactics.engine.model;

import com.tactics.engine.buff.BuffInstance;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete state of an ongoing match.
 * Immutable: all fields are final and collections are unmodifiable.
 *
 * V3 extends this with:
 * - buffTiles: BUFF tiles on the board
 * - obstacles: Obstacles blocking movement
 * - currentRound: Current round number
 * - pendingDeathChoice: Awaiting player decision after minion death
 * - player1TurnEnded/player2TurnEnded: Track turn completion for round processing
 */
public class GameState {

    // V1/V2 Core fields
    private final Board board;
    private final List<Unit> units;
    private final PlayerId currentPlayer;
    private final boolean isGameOver;
    private final PlayerId winner;
    private final Map<String, List<BuffInstance>> unitBuffs;

    // V3 Map element fields
    private final List<BuffTile> buffTiles;
    private final List<Obstacle> obstacles;

    // V3 Game flow fields
    private final int currentRound;
    private final DeathChoice pendingDeathChoice;  // Awaiting player choice after minion death
    private final boolean player1TurnEnded;        // P1 has ended their turn this round
    private final boolean player2TurnEnded;        // P2 has ended their turn this round

    /**
     * V1 constructor - no buffs.
     */
    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner) {
        this(board, units, currentPlayer, isGameOver, winner, Collections.emptyMap());
    }

    /**
     * V2 constructor - with buffs but no V3 fields.
     */
    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner,
                     Map<String, List<BuffInstance>> unitBuffs) {
        this(board, units, currentPlayer, isGameOver, winner, unitBuffs,
             Collections.emptyList(), Collections.emptyList(), 1, null, false, false);
    }

    /**
     * V3 constructor without turn ended flags (backward compatible).
     */
    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner,
                     Map<String, List<BuffInstance>> unitBuffs,
                     List<BuffTile> buffTiles, List<Obstacle> obstacles,
                     int currentRound, DeathChoice pendingDeathChoice) {
        this(board, units, currentPlayer, isGameOver, winner, unitBuffs,
             buffTiles, obstacles, currentRound, pendingDeathChoice, false, false);
    }

    /**
     * V3 full constructor with all fields.
     */
    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner,
                     Map<String, List<BuffInstance>> unitBuffs,
                     List<BuffTile> buffTiles, List<Obstacle> obstacles,
                     int currentRound, DeathChoice pendingDeathChoice,
                     boolean player1TurnEnded, boolean player2TurnEnded) {
        this.board = board;
        this.units = units != null ? Collections.unmodifiableList(units) : Collections.emptyList();
        this.currentPlayer = currentPlayer;
        this.isGameOver = isGameOver;
        this.winner = winner;
        this.unitBuffs = unitBuffs != null ? Collections.unmodifiableMap(unitBuffs) : Collections.emptyMap();
        this.buffTiles = buffTiles != null ? Collections.unmodifiableList(buffTiles) : Collections.emptyList();
        this.obstacles = obstacles != null ? Collections.unmodifiableList(obstacles) : Collections.emptyList();
        this.currentRound = currentRound;
        this.pendingDeathChoice = pendingDeathChoice;
        this.player1TurnEnded = player1TurnEnded;
        this.player2TurnEnded = player2TurnEnded;
    }

    // V1/V2 Core getters

    public Board getBoard() {
        return board;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public PlayerId getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public PlayerId getWinner() {
        return winner;
    }

    public Map<String, List<BuffInstance>> getUnitBuffs() {
        return unitBuffs;
    }

    // V3 Map element getters

    public List<BuffTile> getBuffTiles() {
        return buffTiles;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    // V3 Game flow getters

    public int getCurrentRound() {
        return currentRound;
    }

    public DeathChoice getPendingDeathChoice() {
        return pendingDeathChoice;
    }

    public boolean isPlayer1TurnEnded() {
        return player1TurnEnded;
    }

    public boolean isPlayer2TurnEnded() {
        return player2TurnEnded;
    }

    // Helper methods

    /**
     * Check if there is a pending death choice awaiting player decision.
     */
    public boolean hasPendingDeathChoice() {
        return pendingDeathChoice != null;
    }

    /**
     * Check if both players have ended their turn (ready for round processing).
     */
    public boolean isBothPlayersEndedTurn() {
        return player1TurnEnded && player2TurnEnded;
    }

    /**
     * Check if a position has an obstacle.
     */
    public boolean hasObstacleAt(Position position) {
        return obstacles.stream().anyMatch(o -> o.getPosition().equals(position));
    }

    /**
     * Check if a position has a BUFF tile.
     */
    public boolean hasBuffTileAt(Position position) {
        return buffTiles.stream().anyMatch(t -> t.getPosition().equals(position) && !t.isTriggered());
    }

    /**
     * Get the BUFF tile at a position, if any.
     */
    public BuffTile getBuffTileAt(Position position) {
        return buffTiles.stream()
                .filter(t -> t.getPosition().equals(position) && !t.isTriggered())
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the obstacle at a position, if any.
     */
    public Obstacle getObstacleAt(Position position) {
        return obstacles.stream()
                .filter(o -> o.getPosition().equals(position))
                .findFirst()
                .orElse(null);
    }

    // =========================================================================
    // Immutable "with" methods for creating modified copies
    // =========================================================================

    /**
     * Create a copy with updated units.
     */
    public GameState withUnits(List<Unit> newUnits) {
        return new GameState(board, newUnits, currentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated current player.
     */
    public GameState withCurrentPlayer(PlayerId newCurrentPlayer) {
        return new GameState(board, units, newCurrentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated game over state.
     */
    public GameState withGameOver(boolean newIsGameOver, PlayerId newWinner) {
        return new GameState(board, units, currentPlayer, newIsGameOver, newWinner, unitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated unit buffs.
     */
    public GameState withUnitBuffs(Map<String, List<BuffInstance>> newUnitBuffs) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, newUnitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated buff tiles.
     */
    public GameState withBuffTiles(List<BuffTile> newBuffTiles) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs,
                             newBuffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated obstacles.
     */
    public GameState withObstacles(List<Obstacle> newObstacles) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, newObstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated round number.
     */
    public GameState withCurrentRound(int newCurrentRound) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, obstacles, newCurrentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated pending death choice.
     */
    public GameState withPendingDeathChoice(DeathChoice newPendingDeathChoice) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, obstacles, currentRound, newPendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with updated turn ended flags.
     */
    public GameState withTurnEndedFlags(boolean newPlayer1TurnEnded, boolean newPlayer2TurnEnded) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             newPlayer1TurnEnded, newPlayer2TurnEnded);
    }

    /**
     * Create a copy with multiple field updates (common pattern after actions).
     * Useful when updating units, buffs, and game over state together.
     */
    public GameState withUpdates(List<Unit> newUnits, Map<String, List<BuffInstance>> newUnitBuffs,
                                  boolean newIsGameOver, PlayerId newWinner) {
        return new GameState(board, newUnits, currentPlayer, newIsGameOver, newWinner, newUnitBuffs,
                             buffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy with units, buffs, buff tiles, and game over state updated.
     * Useful after move actions that trigger buff tiles.
     */
    public GameState withMoveResult(List<Unit> newUnits, Map<String, List<BuffInstance>> newUnitBuffs,
                                     List<BuffTile> newBuffTiles, boolean newIsGameOver, PlayerId newWinner) {
        return new GameState(board, newUnits, currentPlayer, newIsGameOver, newWinner, newUnitBuffs,
                             newBuffTiles, obstacles, currentRound, pendingDeathChoice,
                             player1TurnEnded, player2TurnEnded);
    }

    /**
     * Create a copy for round end: increment round, reset turn flags, update units/buffs.
     */
    public GameState withRoundEnd(List<Unit> newUnits, Map<String, List<BuffInstance>> newUnitBuffs,
                                   List<BuffTile> newBuffTiles, List<Obstacle> newObstacles,
                                   PlayerId newCurrentPlayer, boolean newIsGameOver, PlayerId newWinner) {
        return new GameState(board, newUnits, newCurrentPlayer, newIsGameOver, newWinner, newUnitBuffs,
                             newBuffTiles, newObstacles, currentRound + 1, pendingDeathChoice,
                             false, false);
    }
}
