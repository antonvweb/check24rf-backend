package org.example.mcoService.exception;

public class FatalMcoException extends McoException {

    public FatalMcoException(String errorMessage, McoErrorCode errorCode) {
        super(errorCode, errorMessage);
    }

    public FatalMcoException(McoErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}