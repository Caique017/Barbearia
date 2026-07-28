package com.barbearia.api.exceptions;

public class BarbeiroNaoEncontradoException extends RuntimeException {
    public BarbeiroNaoEncontradoException(String message) {
        super(message);
    }
}
