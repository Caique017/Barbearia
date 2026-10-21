package com.barbearia.api.barbeiro;

public class BarbeiroNaoEncontradoException extends RuntimeException {
    public BarbeiroNaoEncontradoException(String message) {
        super(message);
    }
}
