package com.tactics.server.dto;

/**
 * Timer information included in messages to clients.
 * Based on TIMER_DESIGN_DECISIONS.md WebSocket message formats.
 */
public class TimerPayload {

    private final long actionStartTime;
    private final long timeoutMs;
    private final String timerType;

    public TimerPayload(long actionStartTime, long timeoutMs, String timerType) {
        this.actionStartTime = actionStartTime;
        this.timeoutMs = timeoutMs;
        this.timerType = timerType;
    }

    /**
     * Unix timestamp (ms) when the timer started.
     * Clients use this to calculate remaining time.
     */
    public long getActionStartTime() {
        return actionStartTime;
    }

    /**
     * Timer duration in milliseconds.
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Timer type: "ACTION", "DEATH_CHOICE", or "DRAFT".
     */
    public String getTimerType() {
        return timerType;
    }
}
