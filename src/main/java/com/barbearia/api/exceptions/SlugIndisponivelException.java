package com.barbearia.api.exceptions;

public class SlugIndisponivelException extends RuntimeException {
    public SlugIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
