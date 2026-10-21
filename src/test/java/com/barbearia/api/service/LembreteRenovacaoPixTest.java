package com.barbearia.api.service;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.repositories.AssinaturaRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LembreteRenovacaoPixTest {

    @Test
    void avisaQuemVenceEmTresDiasEMarcaParaNaoRepetir() {
        Instant agora = Instant.parse("2026-10-20T12:00:00Z");
        AssinaturaRepository repository = mock(AssinaturaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Usuario dono = Usuario.builder().nome("Ana").email("ana@ex.com").build();
        Assinatura assinatura = Assinatura.emTeste(Barbeiro.builder().usuario(dono).build(),
                agora.minus(Duration.ofDays(30)), Duration.ofDays(7));
        when(repository.vencendoSemLembrete(FormaPagamento.PIX, agora, agora.plus(Duration.ofDays(3))))
                .thenReturn(List.of(assinatura));

        int enviados = new LembreteRenovacaoPix(repository, emailService, Clock.fixed(agora, ZoneOffset.UTC),
                "https://site").enviar();

        assertThat(enviados).isEqualTo(1);
        assertThat(assinatura.getLembreteEnviadoEm()).isEqualTo(agora);
        verify(emailService).enviarLembreteRenovacao("ana@ex.com", "Ana", "https://site/barbeiro/plano");
    }
}
