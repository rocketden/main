package com.rocketden.main.exception.api;

import com.rocketden.main.exception.RoomError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    // Catches all ApiExceptions and returns proper error response to client
    @ExceptionHandler(ApiException.class)
    protected ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        ApiError apiError = e.getError();
        return new ResponseEntity<>(apiError.getResponse(), apiError.getStatus());
    }

    // Handle failure to convert from string to enum (e.g. "easy" -> Difficulty.EASY)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        ApiError apiError = RoomError.BAD_SETTING;
        return new ResponseEntity<>(apiError.getResponse(), apiError.getStatus());
    }
}
