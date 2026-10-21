package com.barbearia.api.exceptions;

public class AgendamentoNaoEncontradoException extends RuntimeException {
    public AgendamentoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
