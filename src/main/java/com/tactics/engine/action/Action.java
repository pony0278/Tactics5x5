package com.tactics.engine.action;

import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;

import java.util.Objects;

/**
 * Represents a player-issued command.
 *
 * V3 extensions:
 * - actingUnitId: The unit performing the action (for skill/obstacle actions)
 * - skillTargetUnitId: Target unit for skills that target units
 * - deathChoiceType: Player's choice after minion death (SPAWN_OBSTACLE or SPAWN_BUFF_TILE)
 */
public class Action {

    // V1/V2 Core fields
    private final ActionType type;
    private final PlayerId playerId;
    private final Position targetPosition;
    private final String targetUnitId;

    // V3 Extended fields
    private final String actingUnitId;                    // Unit performing the action
    private final String skillTargetUnitId;              // Target unit for skills
    private final DeathChoice.ChoiceType deathChoiceType; // For DEATH_CHOICE action

    /**
     * V1/V2 backward-compatible constructor.
     */
    public Action(ActionType type, PlayerId playerId, Position targetPosition, String targetUnitId) {
        this(type, playerId, targetPosition, targetUnitId, null, null, null);
    }

    /**
     * V3 full constructor with all fields.
     */
    public Action(ActionType type, PlayerId playerId, Position targetPosition, String targetUnitId,
                  String actingUnitId, String skillTargetUnitId, DeathChoice.ChoiceType deathChoiceType) {
        this.type = type;
        this.playerId = playerId;
        this.targetPosition = targetPosition;
        this.targetUnitId = targetUnitId;
        this.actingUnitId = actingUnitId;
        this.skillTargetUnitId = skillTargetUnitId;
        this.deathChoiceType = deathChoiceType;
    }

    // V1/V2 Core getters

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

    // V3 Extended getters

    public String getActingUnitId() {
        return actingUnitId;
    }

    public String getSkillTargetUnitId() {
        return skillTargetUnitId;
    }

    public DeathChoice.ChoiceType getDeathChoiceType() {
        return deathChoiceType;
    }

    // Factory methods for V3 actions

    /**
     * Create a USE_SKILL action.
     */
    public static Action useSkill(PlayerId playerId, String actingUnitId, Position targetPosition, String skillTargetUnitId) {
        return new Action(ActionType.USE_SKILL, playerId, targetPosition, null,
                         actingUnitId, skillTargetUnitId, null);
    }

    /**
     * Create an ATTACK action targeting an obstacle.
     * Any unit can attack obstacles. POWER buff destroys instantly.
     */
    public static Action attackObstacle(PlayerId playerId, Position obstaclePosition) {
        return new Action(ActionType.ATTACK, playerId, obstaclePosition, null,
                         null, null, null);
    }

    /**
     * Create a DEATH_CHOICE action.
     */
    public static Action deathChoice(PlayerId playerId, DeathChoice.ChoiceType choiceType) {
        return new Action(ActionType.DEATH_CHOICE, playerId, null, null,
                         null, null, choiceType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return type == action.type &&
               Objects.equals(playerId, action.playerId) &&
               Objects.equals(targetPosition, action.targetPosition) &&
               Objects.equals(targetUnitId, action.targetUnitId) &&
               Objects.equals(actingUnitId, action.actingUnitId) &&
               Objects.equals(skillTargetUnitId, action.skillTargetUnitId) &&
               deathChoiceType == action.deathChoiceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, playerId, targetPosition, targetUnitId,
                           actingUnitId, skillTargetUnitId, deathChoiceType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Action{type=").append(type);
        sb.append(", playerId=").append(playerId);
        if (targetPosition != null) {
            sb.append(", targetPosition=").append(targetPosition);
        }
        if (targetUnitId != null) {
            sb.append(", targetUnitId='").append(targetUnitId).append("'");
        }
        if (actingUnitId != null) {
            sb.append(", actingUnitId='").append(actingUnitId).append("'");
        }
        if (skillTargetUnitId != null) {
            sb.append(", skillTargetUnitId='").append(skillTargetUnitId).append("'");
        }
        if (deathChoiceType != null) {
            sb.append(", deathChoiceType=").append(deathChoiceType);
        }
        sb.append("}");
        return sb.toString();
    }
}
