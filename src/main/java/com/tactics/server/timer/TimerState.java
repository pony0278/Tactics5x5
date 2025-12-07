package com.tactics.server.timer;

/**
 * Represents the lifecycle state of a timer.
 *
 * State transitions:
 * - IDLE → RUNNING: Timer starts
 * - RUNNING → PAUSED: Death Choice, action resolution, or round end
 * - PAUSED → RUNNING: Resume after pause trigger completes
 * - RUNNING → COMPLETED: Valid action received within time
 * - RUNNING → TIMEOUT: Time expired (including grace period)
 */
public enum TimerState {
    /**
     * Timer has not yet started.
     */
    IDLE,

    /**
     * Timer is actively counting down.
     */
    RUNNING,

    /**
     * Timer is temporarily suspended (e.g., during Death Choice).
     */
    PAUSED,

    /**
     * Timer was stopped by a valid action (no timeout).
     */
    COMPLETED,

    /**
     * Timer expired without a valid action (timeout occurred).
     */
    TIMEOUT
}
