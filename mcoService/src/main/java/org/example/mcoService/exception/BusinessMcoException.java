package org.example.mcoService.exception;

public class BusinessMcoException extends McoException {

    public BusinessMcoException(String errorMessage, McoErrorCode errorCode) {
        super(errorCode, errorMessage);
    }

    public BusinessMcoException(McoErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}