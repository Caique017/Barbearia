package com.barbearia.api.exceptions;

public class TokenRedefinicaoInvalidoException extends RuntimeException {
    public TokenRedefinicaoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
