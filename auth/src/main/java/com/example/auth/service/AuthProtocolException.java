package com.example.auth.service;

public class AuthProtocolException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthProtocolException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthErrorCode errorCode() {
        return errorCode;
    }
}
