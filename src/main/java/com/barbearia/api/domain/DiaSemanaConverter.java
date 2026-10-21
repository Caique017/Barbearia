package com.barbearia.api.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

@Converter
public class DiaSemanaConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dia) {
        return dia == null ? null : dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer valor) {
        return valor == null ? null : DayOfWeek.of(valor);
    }
}
