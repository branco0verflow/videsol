package com.videsol.backend.exception;

import lombok.Getter;

@Getter
public class PilotApiException extends RuntimeException {

    private final String pilotCode;
    private final String pilotMessage;

    public PilotApiException(String pilotCode, String pilotMessage) {
        super("Pilot API error [" + pilotCode + "]: " + pilotMessage);
        this.pilotCode = pilotCode;
        this.pilotMessage = pilotMessage;
    }

    public PilotApiException(String message, Throwable cause) {
        super(message, cause);
        this.pilotCode = "unknown";
        this.pilotMessage = message;
    }
}
