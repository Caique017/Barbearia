package com.barbearia.api.exceptions;

public class ServicoNaoEncontradoException extends RuntimeException {
    public ServicoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
