package org.example.mcoService.exception;

import lombok.Getter;

@Getter
public class McoException extends RuntimeException {

    private final McoErrorCode errorCode;
    private final String errorMessage;

    public McoException(McoErrorCode errorCode, String errorMessage) {
        super(String.format("[%s] %s", errorCode.getCode(), errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}