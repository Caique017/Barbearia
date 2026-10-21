package com.barbearia.api.service;

import com.barbearia.api.repositories.TokenRedefinicaoSenhaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LimpezaTokensRedefinicao {

    static final Duration RETENCAO = Duration.ofDays(1);
    private static final Logger log = LoggerFactory.getLogger(LimpezaTokensRedefinicao.class);

    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final Clock clock;

    @Scheduled(cron = "${api.senha.limpeza-cron:0 0 4 * * *}")
    @Transactional
    public int limpar() {
        int apagados = tokenRepository.apagarAnterioresA(clock.instant().minus(RETENCAO));
        log.info("Limpeza de tokens de redefinição: {} apagado(s)", apagados);
        return apagados;
    }
}
