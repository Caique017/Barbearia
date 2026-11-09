package com.barbearia.api.exceptions;

public class SenhaIncorretaException extends RuntimeException {
    public SenhaIncorretaException(String mensagem) {
        super(mensagem);
    }
}
