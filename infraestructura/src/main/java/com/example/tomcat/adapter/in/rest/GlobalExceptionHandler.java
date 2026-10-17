package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.AuthenticationFailedException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.UsuarioInactivoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(AuthenticationFailedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler({AccesoDenegadoException.class, UsuarioInactivoException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(DomainException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage(), HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private Map<String, Object> errorBody(String message, HttpStatus status) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
