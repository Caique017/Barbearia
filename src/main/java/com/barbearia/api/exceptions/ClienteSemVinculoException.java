package com.barbearia.api.exceptions;

public class ClienteSemVinculoException extends RuntimeException {
    public ClienteSemVinculoException(String mensagem) {
        super(mensagem);
    }
}
