package com.tactics.server.dto;

/**
 * Represents the inner "action" object from WS_PROTOCOL_V1.
 */
public class ActionPayload {

    private final String type;
    private final Integer targetX;
    private final Integer targetY;
    private final String targetUnitId;
    private final String actingUnitId;

    public ActionPayload(String type,
                         Integer targetX,
                         Integer targetY,
                         String targetUnitId,
                         String actingUnitId) {
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetUnitId = targetUnitId;
        this.actingUnitId = actingUnitId;
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

    public String getActingUnitId() {
        return actingUnitId;
    }
}
