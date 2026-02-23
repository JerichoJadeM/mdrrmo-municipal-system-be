package com.isufst.mdrrmosystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();

        Map<String, String> error = new HashMap<>();
        error.put("error", errorMessage);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ExceptionResponses> handleException(ResponseStatusException exc){
        return buildResponseEntity(exc, HttpStatus.valueOf(exc.getStatusCode().value()));

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponses> handleException(Exception exc) {
        return buildResponseEntity(exc, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ExceptionResponses> buildResponseEntity(Exception exc, HttpStatus status){

        ExceptionResponses error = new ExceptionResponses();

        error.setStatus(status.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, status);
    }
}
