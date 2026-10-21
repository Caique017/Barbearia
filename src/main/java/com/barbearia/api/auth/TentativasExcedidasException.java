package com.barbearia.api.auth;

import lombok.Getter;

@Getter
public class TentativasExcedidasException extends RuntimeException {

    private final long segundosParaLiberar;

    public TentativasExcedidasException(String message, long segundosParaLiberar) {
        super(message);
        this.segundosParaLiberar = segundosParaLiberar;
    }
}
