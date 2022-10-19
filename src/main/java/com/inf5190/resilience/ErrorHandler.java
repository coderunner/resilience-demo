package com.inf5190.resilience;

import java.io.InterruptedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {
    Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private record ApiError(int code, String message) {
    };

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleCustomException(RuntimeException re) {
        this.log.warn("Unexpected error: {}", re.getMessage());
        return new ApiError(5500, re.getMessage());
    }

    @ExceptionHandler(InterruptedIOException.class)
    @ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
    public ApiError handleCustomException(InterruptedIOException iie) {
        this.log.info("Operation Timeout");
        return new ApiError(5512, iie.getMessage());
    }
}
