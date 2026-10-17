package com.example.tomcat.exception;

public class AuthenticationFailedException extends DomainException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
