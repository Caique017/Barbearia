package com.barbearia.api.exceptions;

public class AssinaturaInativaException extends RuntimeException {
    public AssinaturaInativaException(String mensagem) {
        super(mensagem);
    }
}
