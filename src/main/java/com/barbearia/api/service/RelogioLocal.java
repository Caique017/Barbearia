package com.barbearia.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class RelogioLocal {

    private final Clock clock;
    private final ZoneId fuso;

    public RelogioLocal(Clock clock, @Value("${api.fuso-horario:America/Sao_Paulo}") String fuso) {
        this.clock = clock;
        this.fuso = ZoneId.of(fuso);
    }

    public LocalDateTime agora() {
        return LocalDateTime.ofInstant(clock.instant(), fuso);
    }
}
