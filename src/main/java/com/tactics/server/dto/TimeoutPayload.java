package com.tactics.server.dto;

import java.util.Map;

/**
 * Payload for timeout notification messages.
 * Sent when a player's timer expires.
 */
public class TimeoutPayload {

    private final String timerType;
    private final String playerId;
    private final PenaltyInfo penalty;
    private final String autoAction;
    private final Map<String, Object> state;
    private final TimerPayload nextTimer;
    private final String nextPlayerId;

    public TimeoutPayload(String timerType, String playerId, PenaltyInfo penalty,
                          String autoAction, Map<String, Object> state,
                          TimerPayload nextTimer, String nextPlayerId) {
        this.timerType = timerType;
        this.playerId = playerId;
        this.penalty = penalty;
        this.autoAction = autoAction;
        this.state = state;
        this.nextTimer = nextTimer;
        this.nextPlayerId = nextPlayerId;
    }

    /**
     * Type of timer that expired: "ACTION", "DEATH_CHOICE", or "DRAFT".
     */
    public String getTimerType() {
        return timerType;
    }

    /**
     * ID of the player who timed out.
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Penalty applied for the timeout (may be null for Death Choice).
     */
    public PenaltyInfo getPenalty() {
        return penalty;
    }

    /**
     * Action automatically executed (e.g., "END_TURN").
     */
    public String getAutoAction() {
        return autoAction;
    }

    /**
     * Updated game state after timeout processing.
     */
    public Map<String, Object> getState() {
        return state;
    }

    /**
     * Timer info for next player's turn (null if game over).
     */
    public TimerPayload getNextTimer() {
        return nextTimer;
    }

    /**
     * ID of the next player (null if game over).
     */
    public String getNextPlayerId() {
        return nextPlayerId;
    }

    /**
     * Penalty information for timeout.
     */
    public static class PenaltyInfo {
        private final String type;
        private final int amount;

        public PenaltyInfo(String type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        /**
         * Type of penalty: "HERO_HP_LOSS".
         */
        public String getType() {
            return type;
        }

        /**
         * Amount of penalty (e.g., 1 HP).
         */
        public int getAmount() {
            return amount;
        }
    }
}
