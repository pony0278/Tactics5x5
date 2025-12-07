package com.tactics.server.timer;

import com.tactics.engine.model.PlayerId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages timer lifecycle for matches.
 *
 * Responsibilities:
 * - Start/stop/pause/resume timers
 * - Track remaining time for each timer
 * - Execute timeout callbacks when timers expire
 * - Provide timer state for reconnection sync
 *
 * Thread-safe: Uses ConcurrentHashMap and synchronized methods.
 */
public class TimerService {

    /**
     * Internal representation of a timer instance.
     */
    private static class TimerInstance {
        final String matchId;
        final TimerType type;
        final PlayerId playerId;
        final long startTimeMs;
        final long durationMs;
        final Runnable timeoutCallback;

        TimerState state;
        long pausedRemainingMs;
        ScheduledFuture<?> scheduledTimeout;

        TimerInstance(String matchId, TimerType type, PlayerId playerId,
                      long startTimeMs, long durationMs, Runnable timeoutCallback) {
            this.matchId = matchId;
            this.type = type;
            this.playerId = playerId;
            this.startTimeMs = startTimeMs;
            this.durationMs = durationMs;
            this.timeoutCallback = timeoutCallback;
            this.state = TimerState.RUNNING;
            this.pausedRemainingMs = -1;
        }
    }

    private final Map<String, TimerInstance> timers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final Supplier<Long> clock;

    /**
     * Creates a TimerService with default system clock.
     */
    public TimerService() {
        this(System::currentTimeMillis);
    }

    /**
     * Creates a TimerService with a custom clock (for testing).
     *
     * @param clock supplier providing current time in milliseconds
     */
    public TimerService(Supplier<Long> clock) {
        this.clock = clock;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TimerService-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts an Action Timer for a player's turn.
     *
     * TA-001: Timer starts when called (on YOUR_TURN message send).
     *
     * @param matchId the match identifier
     * @param playerId the player whose turn it is
     * @param timeoutCallback called when timer expires
     * @return the start timestamp in milliseconds
     */
    public long startActionTimer(String matchId, PlayerId playerId, Runnable timeoutCallback) {
        return startTimer(matchId, TimerType.ACTION, playerId,
                TimerConfig.ACTION_TIMEOUT_MS, timeoutCallback);
    }

    /**
     * Starts a Death Choice Timer.
     *
     * @param matchId the match identifier
     * @param playerId the owner of the dead minion (who makes the choice)
     * @param timeoutCallback called when timer expires
     * @return the start timestamp in milliseconds
     */
    public long startDeathChoiceTimer(String matchId, PlayerId playerId, Runnable timeoutCallback) {
        return startTimer(matchId, TimerType.DEATH_CHOICE, playerId,
                TimerConfig.DEATH_CHOICE_TIMEOUT_MS, timeoutCallback);
    }

    /**
     * Starts a Draft Timer.
     *
     * @param matchId the match identifier
     * @param timeoutCallback called when timer expires
     * @return the start timestamp in milliseconds
     */
    public long startDraftTimer(String matchId, Runnable timeoutCallback) {
        return startTimer(matchId, TimerType.DRAFT, null,
                TimerConfig.DRAFT_TIMEOUT_MS, timeoutCallback);
    }

    /**
     * Generic timer start method.
     */
    private long startTimer(String matchId, TimerType type, PlayerId playerId,
                            long durationMs, Runnable timeoutCallback) {
        String key = getTimerKey(matchId, type);

        // Cancel existing timer if any
        cancelTimer(matchId, type);

        long startTime = clock.get();
        TimerInstance timer = new TimerInstance(matchId, type, playerId,
                startTime, durationMs, timeoutCallback);

        // Schedule timeout (duration + grace period)
        long totalDelayMs = durationMs + TimerConfig.GRACE_PERIOD_MS;
        timer.scheduledTimeout = scheduler.schedule(() -> {
            handleTimeout(key);
        }, totalDelayMs, TimeUnit.MILLISECONDS);

        timers.put(key, timer);
        return startTime;
    }

    /**
     * Stops a timer when a valid action is received.
     *
     * TA-002: Valid action stops the timer with no penalty.
     *
     * @param matchId the match identifier
     * @param type the timer type to stop
     * @return true if timer was stopped, false if not found or already stopped
     */
    public boolean completeTimer(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);

        if (timer == null || timer.state != TimerState.RUNNING) {
            return false;
        }

        timer.state = TimerState.COMPLETED;
        if (timer.scheduledTimeout != null) {
            timer.scheduledTimeout.cancel(false);
        }
        return true;
    }

    /**
     * Pauses the Action Timer (e.g., during Death Choice).
     *
     * @param matchId the match identifier
     * @return remaining time in milliseconds, or -1 if not running
     */
    public long pauseActionTimer(String matchId) {
        String key = getTimerKey(matchId, TimerType.ACTION);
        TimerInstance timer = timers.get(key);

        if (timer == null || timer.state != TimerState.RUNNING) {
            return -1;
        }

        // Calculate remaining time BEFORE changing state
        long elapsed = clock.get() - timer.startTimeMs;
        long remaining = Math.max(0, timer.durationMs - elapsed);

        timer.state = TimerState.PAUSED;
        timer.pausedRemainingMs = remaining;

        if (timer.scheduledTimeout != null) {
            timer.scheduledTimeout.cancel(false);
        }

        return timer.pausedRemainingMs;
    }

    /**
     * Resumes the Action Timer after a pause.
     * Note: After Death Choice, timer resets to full duration per design decision #12.
     *
     * @param matchId the match identifier
     * @param resetToFull if true, resets to full 10s; if false, resumes from paused time
     * @return the new start timestamp, or -1 if no paused timer
     */
    public long resumeActionTimer(String matchId, boolean resetToFull) {
        String key = getTimerKey(matchId, TimerType.ACTION);
        TimerInstance oldTimer = timers.get(key);

        if (oldTimer == null || oldTimer.state != TimerState.PAUSED) {
            return -1;
        }

        long duration = resetToFull ? TimerConfig.ACTION_TIMEOUT_MS : oldTimer.pausedRemainingMs;
        return startTimer(matchId, TimerType.ACTION, oldTimer.playerId,
                duration, oldTimer.timeoutCallback);
    }

    /**
     * Cancels and removes a timer without triggering timeout callback.
     *
     * @param matchId the match identifier
     * @param type the timer type to cancel
     */
    public void cancelTimer(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.remove(key);

        if (timer != null && timer.scheduledTimeout != null) {
            timer.scheduledTimeout.cancel(false);
        }
    }

    /**
     * Gets the remaining time for a timer.
     *
     * @param matchId the match identifier
     * @param type the timer type
     * @return remaining time in milliseconds, or -1 if timer not found/not running
     */
    public long getRemainingTime(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);

        if (timer == null) {
            return -1;
        }

        if (timer.state == TimerState.PAUSED) {
            return timer.pausedRemainingMs;
        }

        if (timer.state != TimerState.RUNNING) {
            return -1;
        }

        long elapsed = clock.get() - timer.startTimeMs;
        long remaining = timer.durationMs - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Gets the current state of a timer.
     *
     * @param matchId the match identifier
     * @param type the timer type
     * @return the timer state, or null if not found
     */
    public TimerState getTimerState(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);
        return timer != null ? timer.state : null;
    }

    /**
     * Gets the start timestamp of a timer.
     *
     * @param matchId the match identifier
     * @param type the timer type
     * @return the start timestamp, or -1 if not found
     */
    public long getStartTime(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);
        return timer != null ? timer.startTimeMs : -1;
    }

    /**
     * Gets the timeout duration for a timer.
     *
     * @param matchId the match identifier
     * @param type the timer type
     * @return the timeout duration in ms, or -1 if not found
     */
    public long getTimeoutMs(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);
        return timer != null ? timer.durationMs : -1;
    }

    /**
     * Checks if an action is within the grace period.
     * Used to accept late actions that arrive just after timeout.
     *
     * @param matchId the match identifier
     * @param type the timer type
     * @return true if within grace period, false otherwise
     */
    public boolean isWithinGracePeriod(String matchId, TimerType type) {
        String key = getTimerKey(matchId, type);
        TimerInstance timer = timers.get(key);

        if (timer == null) {
            return false;
        }

        long elapsed = clock.get() - timer.startTimeMs;
        long overtime = elapsed - timer.durationMs;

        return overtime > 0 && overtime <= TimerConfig.GRACE_PERIOD_MS;
    }

    /**
     * Handles timer timeout.
     * TA-003: Timeout triggers the callback.
     */
    private void handleTimeout(String key) {
        TimerInstance timer = timers.get(key);

        if (timer == null || timer.state != TimerState.RUNNING) {
            return;
        }

        timer.state = TimerState.TIMEOUT;

        if (timer.timeoutCallback != null) {
            timer.timeoutCallback.run();
        }
    }

    /**
     * Generates a unique key for a timer.
     */
    private String getTimerKey(String matchId, TimerType type) {
        return matchId + ":" + type.name();
    }

    /**
     * Shuts down the scheduler. Call when the server stops.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
