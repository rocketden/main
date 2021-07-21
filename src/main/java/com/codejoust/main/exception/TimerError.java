package com.codejoust.main.exception;

import lombok.Getter;

import com.codejoust.main.exception.api.ApiError;
import com.codejoust.main.exception.api.ApiErrorResponse;

import org.springframework.http.HttpStatus;

@Getter
public enum TimerError implements ApiError {

    INVALID_DURATION(HttpStatus.BAD_REQUEST, "Please enter a valid duration between 1-60 minutes."),
    NULL_SETTING(HttpStatus.BAD_REQUEST, "The relevant game settings must not be null.");

    private final HttpStatus status;
    private final ApiErrorResponse response;

    TimerError(HttpStatus status, String message) {
        this.status = status;
        this.response = new ApiErrorResponse(message, this.name());
    }

    public String getMessage() {
        return this.response.getMessage();
    }
}
