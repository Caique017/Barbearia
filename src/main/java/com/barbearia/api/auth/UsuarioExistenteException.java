package com.barbearia.api.auth;

public class UsuarioExistenteException extends RuntimeException {

    public UsuarioExistenteException(String message) {
        super(message);
    }
}
