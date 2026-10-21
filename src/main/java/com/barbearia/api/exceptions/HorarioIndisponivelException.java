package com.barbearia.api.exceptions;

public class HorarioIndisponivelException extends RuntimeException {
    public HorarioIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
