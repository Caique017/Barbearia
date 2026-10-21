package com.barbearia.api.compartilhado.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    private int maximo;

    @Override
    public void initialize(MaxBytes anotacao) {
        this.maximo = anotacao.value();
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= maximo;
    }
}
