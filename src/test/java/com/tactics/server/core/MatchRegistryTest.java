package com.tactics.server.core;

import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for MatchRegistry
 * Based on SERVER_CORE_TESTPLAN_V1.md (MR-Series)
 */
class MatchRegistryTest {

    private MatchRegistry registry;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        registry = new MatchRegistry(new HashMap<>());
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    private GameState createDefaultState() {
        return new GameState(board, new ArrayList<>(), p1, false, null);
    }

    private GameState createStateWithPlayer(PlayerId currentPlayer) {
        return new GameState(board, new ArrayList<>(), currentPlayer, false, null);
    }

    // ========== MR-Series: MatchRegistry Tests ==========

    @Nested
    @DisplayName("MR-Series: MatchRegistry Tests")
    class MatchRegistryTests {

        @Test
        @DisplayName("MR1 - getMatch on empty registry returns null")
        void mr1_getMatchOnEmptyRegistryReturnsNull() {
            // Given: empty registry (set up in @BeforeEach)

            // When
            Match result = registry.getMatch("match-1");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("MR2 - createMatch adds new match")
        void mr2_createMatchAddsNewMatch() {
            // Given
            GameState initialState = createDefaultState();

            // When
            Match createdMatch = registry.createMatch("match-1", initialState);

            // Then
            assertNotNull(createdMatch);

            Match retrieved = registry.getMatch("match-1");
            assertNotNull(retrieved);
            assertEquals("match-1", retrieved.getMatchId().getValue());
            assertSame(initialState, retrieved.getState());
            assertNotNull(retrieved.getConnections());
            assertTrue(retrieved.getConnections().isEmpty());
        }

        @Test
        @DisplayName("MR3 - createMatch overwrites existing match")
        void mr3_createMatchOverwritesExistingMatch() {
            // Given: existing match
            GameState oldState = createStateWithPlayer(p1);
            registry.createMatch("match-1", oldState);

            GameState newState = createStateWithPlayer(p2);

            // When
            registry.createMatch("match-1", newState);

            // Then
            Match retrieved = registry.getMatch("match-1");
            assertSame(newState, retrieved.getState());
            assertNotSame(oldState, retrieved.getState());
        }

        @Test
        @DisplayName("MR4 - updateMatchState replaces match state (immutability)")
        void mr4_updateMatchStateReplacesState() {
            // Given
            GameState oldState = createStateWithPlayer(p1);
            registry.createMatch("match-1", oldState);

            GameState newState = createStateWithPlayer(p2);

            // When
            registry.updateMatchState("match-1", newState);

            // Then
            Match retrieved = registry.getMatch("match-1");
            assertSame(newState, retrieved.getState());

            // Verify old state is unchanged (immutability)
            assertEquals("P1", oldState.getCurrentPlayer().getValue());
            assertEquals("P2", newState.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("MR4 - updateMatchState on non-existing match does nothing")
        void mr4_updateMatchStateOnNonExistingMatchDoesNothing() {
            // Given: no match exists
            GameState newState = createDefaultState();

            // When
            registry.updateMatchState("non-existing", newState);

            // Then
            assertNull(registry.getMatch("non-existing"));
        }

        @Test
        @DisplayName("MR5 - listMatches returns all active matches")
        void mr5_listMatchesReturnsAllActiveMatches() {
            // Given
            GameState state1 = createDefaultState();
            GameState state2 = createDefaultState();
            registry.createMatch("match-1", state1);
            registry.createMatch("match-2", state2);

            // When
            Collection<Match> matches = registry.listMatches();

            // Then
            assertEquals(2, matches.size());

            Set<String> matchIds = new HashSet<>();
            for (Match m : matches) {
                matchIds.add(m.getMatchId().getValue());
            }
            assertTrue(matchIds.contains("match-1"));
            assertTrue(matchIds.contains("match-2"));
        }

        @Test
        @DisplayName("MR5 - listMatches on empty registry returns empty collection")
        void mr5_listMatchesOnEmptyRegistryReturnsEmpty() {
            // Given: empty registry

            // When
            Collection<Match> matches = registry.listMatches();

            // Then
            assertTrue(matches.isEmpty());
        }
    }
}
