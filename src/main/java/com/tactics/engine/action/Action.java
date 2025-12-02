package com.tactics.engine.action;

import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;

/**
 * Represents a player-issued command.
 */
public class Action {

    private final ActionType type;
    private final PlayerId playerId;
    private final Position targetPosition;
    private final String targetUnitId;

    public Action(ActionType type, PlayerId playerId, Position targetPosition, String targetUnitId) {
        this.type = type;
        this.playerId = playerId;
        this.targetPosition = targetPosition;
        this.targetUnitId = targetUnitId;
    }

    public ActionType getType() {
        return type;
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public Position getTargetPosition() {
        return targetPosition;
    }

    public String getTargetUnitId() {
        return targetUnitId;
    }
}
