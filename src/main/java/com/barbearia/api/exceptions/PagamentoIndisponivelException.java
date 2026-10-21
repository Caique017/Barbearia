package com.barbearia.api.exceptions;

public class PagamentoIndisponivelException extends RuntimeException {
    public PagamentoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
