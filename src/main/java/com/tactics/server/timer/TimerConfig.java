package com.tactics.server.timer;

/**
 * Timer duration constants for the game.
 */
public final class TimerConfig {

    private TimerConfig() {
        // Utility class - no instantiation
    }

    /**
     * Action Timer duration: 10 seconds.
     */
    public static final long ACTION_TIMEOUT_MS = 10_000L;

    /**
     * Death Choice Timer duration: 5 seconds.
     */
    public static final long DEATH_CHOICE_TIMEOUT_MS = 5_000L;

    /**
     * Draft Timer duration: 60 seconds.
     */
    public static final long DRAFT_TIMEOUT_MS = 60_000L;

    /**
     * Network grace period: 500ms tolerance after timer expiry.
     * Actions received within this window are still accepted.
     */
    public static final long GRACE_PERIOD_MS = 500L;

    /**
     * Get the timeout duration for a given timer type.
     *
     * @param timerType the type of timer
     * @return timeout duration in milliseconds
     */
    public static long getTimeoutMs(TimerType timerType) {
        return switch (timerType) {
            case ACTION -> ACTION_TIMEOUT_MS;
            case DEATH_CHOICE -> DEATH_CHOICE_TIMEOUT_MS;
            case DRAFT -> DRAFT_TIMEOUT_MS;
        };
    }
}
