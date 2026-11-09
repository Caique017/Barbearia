package com.barbearia.api.exceptions;

public class DescadastroInvalidoException extends RuntimeException {
    public DescadastroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
