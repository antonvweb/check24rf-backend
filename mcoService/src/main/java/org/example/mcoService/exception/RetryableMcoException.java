package org.example.mcoService.exception;

public class RetryableMcoException extends McoException {

    public RetryableMcoException(String errorCode, McoErrorCode errorMessage) {
        super(errorMessage, errorCode);
    }
}