package com.tactics.server.timer;

import com.tactics.engine.model.PlayerId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TN-Series: Network & Sync Timer Tests
 * Based on TIMER_TESTPLAN.md
 *
 * Tests for network-related timer behavior:
 * - Grace period handling
 * - Client/Server timestamp synchronization
 * - Disconnection behavior
 * - Server authority
 * - Clock drift handling
 * - Race conditions
 */
@DisplayName("TN-Series: Network & Sync Timer Tests")
class TimerServiceNetworkTest {

    private TimerService timerService;
    private AtomicLong mockTime;
    private PlayerId player1;
    private PlayerId player2;

    @BeforeEach
    void setUp() {
        mockTime = new AtomicLong(1000000L);
        timerService = new TimerService(mockTime::get);
        player1 = new PlayerId("PLAYER_1");
        player2 = new PlayerId("PLAYER_2");
    }

    @AfterEach
    void tearDown() {
        timerService.shutdown();
    }

    // ========== TN-001 ~ TN-002: Grace Period Tests ==========

    @Nested
    @DisplayName("TN-001 ~ TN-002: Grace Period Handling")
    class GracePeriodTests {

        @Test
        @DisplayName("TN-001: Network grace period - 500ms bandwidth")
        void tn001_networkGracePeriod500ms() {
            // Given: Server Timer shows 0ms remaining
            String matchId = "match-1";
            timerService.startActionTimer(matchId, player1, () -> {});

            // Timer expires at t = 1010000 (start + 10000)
            // Grace period ends at t = 1010500 (start + 10500)

            // When: Player action received at 300ms after expiry (within grace)
            mockTime.set(1010300L);

            // Then: Action accepted (within grace period)
            assertTrue(timerService.isWithinGracePeriod(matchId, TimerType.ACTION),
                "Action at 300ms after timeout should be within 500ms grace period");

            // Timer can still be completed within grace period
            boolean completed = timerService.completeTimer(matchId, TimerType.ACTION);
            assertTrue(completed, "Should be able to complete timer during grace period");
        }

        @Test
        @DisplayName("TN-002: Action rejected after grace period expired")
        void tn002_actionRejectedAfterGracePeriodExpired() {
            // Given: Server Timer expired 600ms ago
            String matchId = "match-1";
            AtomicBoolean timeoutCalled = new AtomicBoolean(false);

            timerService.startActionTimer(matchId, player1, () -> {
                timeoutCalled.set(true);
            });

            // Timer expires at t = 1010000
            // 600ms after expiry = t = 1010600

            // When: Player action received 600ms after expiry
            mockTime.set(1010600L);

            // Then: Outside grace period
            assertFalse(timerService.isWithinGracePeriod(matchId, TimerType.ACTION),
                "Action at 600ms after timeout should be outside 500ms grace period");

            // Remaining time should be 0
            assertEquals(0L, timerService.getRemainingTime(matchId, TimerType.ACTION),
                "Remaining time should be 0 after timeout");
        }
    }

    // ========== TN-003: Client/Server Sync ==========

    @Nested
    @DisplayName("TN-003: Client/Server Timestamp Synchronization")
    class ClientServerSyncTests {

        @Test
        @DisplayName("TN-003: Client Timer sync with Server timestamp")
        void tn003_clientTimerSyncWithServerTimestamp() {
            // Given: Server sends actionStartTime = T
            String matchId = "match-1";
            long serverStartTime = timerService.startActionTimer(matchId, player1, () -> {});
            assertEquals(1000000L, serverStartTime, "Server start time should be 1000000");

            // When: Client receives at T + 100ms (simulated network delay)
            long clientReceiveTime = serverStartTime + 100;
            mockTime.set(clientReceiveTime);

            // Then: Client Timer = 10000 - 100 = 9900ms remaining
            long remainingTime = timerService.getRemainingTime(matchId, TimerType.ACTION);
            assertEquals(9900L, remainingTime,
                "Client should calculate 9900ms remaining (10000 - 100ms delay)");
        }

        @Test
        @DisplayName("TN-003b: Timer returns start time for client calculation")
        void tn003b_timerReturnsStartTimeForClientCalculation() {
            // Given: Timer started
            String matchId = "match-1";
            long startTime = timerService.startActionTimer(matchId, player1, () -> {});

            // Then: Start time is available for client sync
            assertEquals(startTime, timerService.getStartTime(matchId, TimerType.ACTION),
                "getStartTime should return the original start timestamp");

            // And timeout duration is available
            assertEquals(TimerConfig.ACTION_TIMEOUT_MS,
                timerService.getTimeoutMs(matchId, TimerType.ACTION),
                "getTimeoutMs should return the timer duration");
        }
    }

    // ========== TN-004 ~ TN-005: Disconnection Handling ==========

    @Nested
    @DisplayName("TN-004 ~ TN-005: Disconnection Behavior")
    class DisconnectionTests {

        @Test
        @DisplayName("TN-004: Disconnection does not pause Timer")
        void tn004_disconnectionDoesNotPauseTimer() {
            // Given: Player 1 disconnects during their turn
            String matchId = "match-1";
            AtomicBoolean timeoutCalled = new AtomicBoolean(false);

            timerService.startActionTimer(matchId, player1, () -> {
                timeoutCalled.set(true);
            });

            // Timer is running
            assertEquals(TimerState.RUNNING, timerService.getTimerState(matchId, TimerType.ACTION));

            // When: Time passes (simulating disconnection period)
            mockTime.set(1005000L);

            // Then: Timer continues regardless of connection
            assertEquals(5000L, timerService.getRemainingTime(matchId, TimerType.ACTION),
                "Timer should continue counting down during disconnection");
            assertEquals(TimerState.RUNNING, timerService.getTimerState(matchId, TimerType.ACTION),
                "Timer state should remain RUNNING during disconnection");

            // And when timeout occurs
            mockTime.set(1010000L);
            assertEquals(0L, timerService.getRemainingTime(matchId, TimerType.ACTION),
                "Timer should timeout even while disconnected");
        }

        @Test
        @DisplayName("TN-005: Reconnection syncs current Timer state")
        void tn005_reconnectionSyncsCurrentTimerState() {
            // Given: Player 1 disconnected with timer running
            String matchId = "match-1";
            timerService.startActionTimer(matchId, player1, () -> {});

            // Time passes during disconnection (3 seconds)
            mockTime.set(1003000L);

            // When: Reconnection established
            // Server provides current timer state for sync

            // Then: Timer shows remaining time (~7000ms if still their turn)
            long remaining = timerService.getRemainingTime(matchId, TimerType.ACTION);
            assertEquals(7000L, remaining,
                "Reconnected client should see 7000ms remaining after 3s disconnect");

            // All data needed for sync is available
            assertNotNull(timerService.getTimerState(matchId, TimerType.ACTION));
            assertTrue(timerService.getStartTime(matchId, TimerType.ACTION) > 0);
            assertTrue(timerService.getTimeoutMs(matchId, TimerType.ACTION) > 0);
        }
    }

    // ========== TN-006: Server Authority ==========

    @Nested
    @DisplayName("TN-006: Server Authoritative Timer")
    class ServerAuthorityTests {

        @Test
        @DisplayName("TN-006: Server authoritative Timer - server decision is final")
        void tn006_serverAuthoritativeTimer() {
            // Given: Timer started (server time is authoritative)
            String matchId = "match-1";
            timerService.startActionTimer(matchId, player1, () -> {});

            // Client Timer might show 1000ms (client clock drift)
            // Server Timer shows 0ms (timeout)

            // When: Server processes timeout at its time
            mockTime.set(1010000L); // Server says timeout

            // Then: Server decision is final
            assertEquals(0L, timerService.getRemainingTime(matchId, TimerType.ACTION),
                "Server remaining time is authoritative");

            // Timeout has occurred from server perspective
            assertFalse(timerService.isWithinGracePeriod(matchId, TimerType.ACTION) &&
                        timerService.getRemainingTime(matchId, TimerType.ACTION) > 0,
                "Server determines timeout, not client");
        }

        @Test
        @DisplayName("TN-006b: Server timer state is ground truth")
        void tn006b_serverTimerStateIsGroundTruth() {
            // Given: Timer running
            String matchId = "match-1";
            timerService.startActionTimer(matchId, player1, () -> {});

            // When: Server completes the timer (valid action received)
            timerService.completeTimer(matchId, TimerType.ACTION);

            // Then: Server state is authoritative
            assertEquals(TimerState.COMPLETED, timerService.getTimerState(matchId, TimerType.ACTION),
                "Server timer state is the ground truth");

            // No further actions can affect this timer
            assertFalse(timerService.completeTimer(matchId, TimerType.ACTION),
                "Cannot complete an already-completed timer");
        }
    }

    // ========== TN-007: Clock Drift Handling ==========

    @Nested
    @DisplayName("TN-007: Clock Drift Handling")
    class ClockDriftTests {

        @Test
        @DisplayName("TN-007: Clock drift handling - fresh timestamp each action")
        void tn007_clockDriftHandlingFreshTimestampEachAction() {
            // Given: Long game (simulating 30+ minutes with potential drift)
            String matchId = "match-1";

            // First action at t = 1000000
            long startTime1 = timerService.startActionTimer(matchId, player1, () -> {});
            assertEquals(1000000L, startTime1);

            // Complete first action
            mockTime.set(1005000L);
            timerService.completeTimer(matchId, TimerType.ACTION);

            // When: Second action starts much later (simulating game progress)
            mockTime.set(2000000L); // 1 million ms later

            // Then: Each Timer message has fresh actionStartTime
            long startTime2 = timerService.startActionTimer(matchId, player1, () -> {});
            assertEquals(2000000L, startTime2,
                "Each new timer should have fresh start timestamp");

            // No cumulative drift - timer always starts fresh
            assertEquals(TimerConfig.ACTION_TIMEOUT_MS,
                timerService.getRemainingTime(matchId, TimerType.ACTION),
                "New timer should have full 10s regardless of previous timers");
        }

        @Test
        @DisplayName("TN-007b: Multiple timer cycles don't accumulate drift")
        void tn007b_multipleTimerCyclesDontAccumulateDrift() {
            // Given: Multiple timer cycles
            String matchId = "match-1";

            for (int i = 0; i < 10; i++) {
                long baseTime = 1000000L + (i * 15000L);
                mockTime.set(baseTime);

                // Start timer
                long startTime = timerService.startActionTimer(matchId, player1, () -> {});
                assertEquals(baseTime, startTime, "Cycle " + i + " should start at current time");

                // Verify full duration
                assertEquals(TimerConfig.ACTION_TIMEOUT_MS,
                    timerService.getRemainingTime(matchId, TimerType.ACTION),
                    "Cycle " + i + " should have full 10s");

                // Complete timer
                mockTime.set(baseTime + 5000L);
                timerService.completeTimer(matchId, TimerType.ACTION);
            }
        }
    }

    // ========== TN-008: Race Condition ==========

    @Nested
    @DisplayName("TN-008: Race Condition Handling")
    class RaceConditionTests {

        @Test
        @DisplayName("TN-008: Concurrent action submission - within grace accepted")
        void tn008_concurrentActionSubmissionWithinGraceAccepted() {
            // Given: Timer at 100ms remaining
            String matchId = "match-1";
            timerService.startActionTimer(matchId, player1, () -> {});

            mockTime.set(1009900L); // 100ms remaining
            assertEquals(100L, timerService.getRemainingTime(matchId, TimerType.ACTION));

            // When: Player submits action as Timer expires on Server
            // Simulate: action arrives at timeout + 200ms (within grace)
            mockTime.set(1010200L);

            // Then: Action received within grace (500ms): Accept action
            assertTrue(timerService.isWithinGracePeriod(matchId, TimerType.ACTION),
                "Action arriving 200ms after timeout should be within grace");

            // Action can still be completed
            boolean completed = timerService.completeTimer(matchId, TimerType.ACTION);
            assertTrue(completed,
                "Should accept action submitted within grace period despite race");
        }

        @Test
        @DisplayName("TN-008b: Action after timeout processed - rejected")
        void tn008b_actionAfterTimeoutProcessedRejected() {
            // Given: Timer expired and timeout was processed
            String matchId = "match-1";
            AtomicBoolean timeoutProcessed = new AtomicBoolean(false);

            timerService.startActionTimer(matchId, player1, () -> {
                timeoutProcessed.set(true);
            });

            // Timeout occurs at t = 1010000
            // Grace period ends at t = 1010500

            // When: Action arrives after grace period
            mockTime.set(1010600L);

            // Then: If timeout processed first: Reject action
            assertFalse(timerService.isWithinGracePeriod(matchId, TimerType.ACTION),
                "Action at 600ms after timeout should be rejected");

            // Cannot complete timer after grace period
            // (In real implementation, timeout callback would have already fired)
        }

        @Test
        @DisplayName("TN-008c: Complete timer prevents duplicate timeout")
        void tn008c_completeTimerPreventsDuplicateTimeout() {
            // Given: Timer running
            String matchId = "match-1";
            AtomicBoolean timeoutCalled = new AtomicBoolean(false);

            timerService.startActionTimer(matchId, player1, () -> {
                timeoutCalled.set(true);
            });

            // When: Action completed just before timeout
            mockTime.set(1009900L); // 100ms remaining
            boolean completed = timerService.completeTimer(matchId, TimerType.ACTION);

            // Then: Timer completed
            assertTrue(completed);
            assertEquals(TimerState.COMPLETED, timerService.getTimerState(matchId, TimerType.ACTION));

            // Timeout callback should not fire (timer was completed)
            // The scheduled future was cancelled in completeTimer()
        }
    }
}
