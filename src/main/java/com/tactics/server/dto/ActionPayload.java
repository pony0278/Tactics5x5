package com.tactics.server.dto;

/**
 * Represents the inner "action" object from WS_PROTOCOL_V1.
 */
public class ActionPayload {

    private final String type;
    private final Integer targetX;
    private final Integer targetY;
    private final String targetUnitId;

    public ActionPayload(String type,
                         Integer targetX,
                         Integer targetY,
                         String targetUnitId) {
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetUnitId = targetUnitId;
    }

    public String getType() {
        return type;
    }

    public Integer getTargetX() {
        return targetX;
    }

    public Integer getTargetY() {
        return targetY;
    }

    public String getTargetUnitId() {
        return targetUnitId;
    }
}
