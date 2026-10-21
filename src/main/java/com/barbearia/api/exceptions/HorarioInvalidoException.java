package com.barbearia.api.exceptions;

public class HorarioInvalidoException extends RuntimeException {
    public HorarioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
