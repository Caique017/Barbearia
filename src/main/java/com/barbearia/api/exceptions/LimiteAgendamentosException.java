package com.barbearia.api.exceptions;

public class LimiteAgendamentosException extends RuntimeException {
    public LimiteAgendamentosException(String mensagem) {
        super(mensagem);
    }
}
