package com.fypgrading.evaluationservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler
    protected ResponseEntity<ExceptionResponse> handleIllegalStateException(IllegalStateException ex) {
        ExceptionResponse response = new ExceptionResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler
    protected ResponseEntity<ExceptionResponse> handleEntityNotFound(EntityNotFoundException ex) {
        ExceptionResponse response = new ExceptionResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler
    protected ResponseEntity<ExceptionResponse> handleRuntimeException(RuntimeException ex) {
        ExceptionResponse response = new ExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
