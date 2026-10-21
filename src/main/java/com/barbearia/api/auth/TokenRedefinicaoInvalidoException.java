package com.barbearia.api.auth;

public class TokenRedefinicaoInvalidoException extends RuntimeException {
    public TokenRedefinicaoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
