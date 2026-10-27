package com.barbearia.api.exceptions;

public class AssinaturaDuplicadaException extends RuntimeException {
    public AssinaturaDuplicadaException(String mensagem) {
        super(mensagem);
    }
}
