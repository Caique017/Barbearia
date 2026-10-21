package com.barbearia.api.barbeiro;

public class SlugIndisponivelException extends RuntimeException {
    public SlugIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
