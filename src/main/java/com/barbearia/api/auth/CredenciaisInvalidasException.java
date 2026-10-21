package com.barbearia.api.auth;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
