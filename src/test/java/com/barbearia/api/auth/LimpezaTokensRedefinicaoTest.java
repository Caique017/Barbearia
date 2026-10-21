package com.barbearia.api.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimpezaTokensRedefinicaoTest {

    @Test
    void apagaOQueVenceuOuFoiUsadoHaMaisDeUmDia() {
        Instant agora = Instant.parse("2026-10-20T04:00:00Z");
        TokenRedefinicaoSenhaRepository repository = mock(TokenRedefinicaoSenhaRepository.class);
        when(repository.apagarAnterioresA(Instant.parse("2026-10-19T04:00:00Z"))).thenReturn(7);

        int apagados = new LimpezaTokensRedefinicao(repository, Clock.fixed(agora, ZoneOffset.UTC)).limpar();

        assertThat(apagados).isEqualTo(7);
    }
}
