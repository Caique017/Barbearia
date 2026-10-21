package com.barbearia.api.compartilhado.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "api.limite-ip")
public record LimitesPorIp(Regra login, Regra cadastro, Regra senha) {

    public record Regra(int maximo, Duration janela) {
    }
}
