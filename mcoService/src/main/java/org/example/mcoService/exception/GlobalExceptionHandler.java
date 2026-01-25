package org.example.mcoService.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.api.ApiResponse;
import org.example.mcoService.dto.response.DrPlatformError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RetryableMcoException.class)
    public ResponseEntity<ApiResponse<Object>> handleRetryable(RetryableMcoException ex) {
        log.warn("Retryable error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        ex.getErrorMessage(),
                        ex.getErrorCode().getCode(),
                        "Retryable error - please try again later"
                ));
    }

    @ExceptionHandler(BusinessMcoException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessMcoException ex) {
        log.info("Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ex.getErrorMessage(),
                        ex.getErrorCode().getCode(),
                        "Business validation error"
                ));
    }

    @ExceptionHandler(FatalMcoException.class)
    public ResponseEntity<ApiResponse<Object>> handleFatal(FatalMcoException ex) {
        log.error("Fatal error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ex.getErrorMessage(),
                        ex.getErrorCode().getCode(),
                        "Fatal error - contact support"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }

    public static void processDrPlatformError(DrPlatformError error) {
        McoErrorCode errorCode = McoErrorCode.fromCode(error.getCode());
        String message = error.getMessage() != null ? error.getMessage() : "No message provided";

        switch (errorCode.getType()) {
            case RETRYABLE  -> throw new RetryableMcoException(message, errorCode);
            case BUSINESS   -> throw new BusinessMcoException(message, errorCode);
            case FATAL      -> throw new FatalMcoException(message, errorCode);
            default         -> throw new FatalMcoException(message, errorCode);
        }
    }
}