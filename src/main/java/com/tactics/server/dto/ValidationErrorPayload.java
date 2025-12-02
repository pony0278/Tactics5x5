package com.tactics.server.dto;

/**
 * Payload for validation_error messages.
 */
public class ValidationErrorPayload {

    private final String message;
    private final ActionPayload action;

    public ValidationErrorPayload(String message, ActionPayload action) {
        this.message = message;
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public ActionPayload getAction() {
        return action;
    }
}
