package com.barbearia.api.exceptions;

public class BloqueioNaoEncontradoException extends RuntimeException {
    public BloqueioNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
