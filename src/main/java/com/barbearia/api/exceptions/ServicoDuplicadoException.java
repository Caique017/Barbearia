package com.barbearia.api.exceptions;

public class ServicoDuplicadoException extends RuntimeException {
    public ServicoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
