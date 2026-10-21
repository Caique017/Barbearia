package com.barbearia.api.exceptions;

public class TransicaoAgendamentoInvalidaException extends RuntimeException {
    public TransicaoAgendamentoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
