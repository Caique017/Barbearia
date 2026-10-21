package com.barbearia.api.domain;

import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.enums.StatusAssinatura;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaTest {

    private static final Instant CADASTRO = Instant.parse("2026-10-01T12:00:00Z");
    private static final Duration SETE_DIAS = Duration.ofDays(7);
    private static final Duration TRINTA_DIAS = Duration.ofDays(30);

    private static Assinatura nova() {
        return Assinatura.emTeste(new Barbeiro(), CADASTRO, SETE_DIAS);
    }

    @Test
    void testeValeSeteDiasEDepoisVence() {
        Assinatura assinatura = nova();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TESTE);
        assertThat(assinatura.ativa(CADASTRO.plus(Duration.ofDays(6)))).isTrue();
        assertThat(assinatura.ativa(CADASTRO.plus(SETE_DIAS))).isFalse();
    }

    @Test
    void pixPagoDuranteOTesteSomaDepoisDoFimDoTeste() {
        Assinatura assinatura = nova();

        assinatura.pagaNoPix(TRINTA_DIAS, CADASTRO.plus(Duration.ofDays(2)));

        assertThat(assinatura.getAcessoAte()).isEqualTo(CADASTRO.plus(SETE_DIAS).plus(TRINTA_DIAS));
        assertThat(assinatura.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
    }

    @Test
    void pixPagoDepoisDeVencidoContaAPartirDeAgora() {
        Assinatura assinatura = nova();
        Instant muitoDepois = CADASTRO.plus(Duration.ofDays(40));

        assinatura.pagaNoPix(TRINTA_DIAS, muitoDepois);

        assertThat(assinatura.getAcessoAte()).isEqualTo(muitoDepois.plus(TRINTA_DIAS));
    }

    @Test
    void doisPixSeguidosAcumulam() {
        Assinatura assinatura = nova();
        Instant depoisDoTeste = CADASTRO.plus(Duration.ofDays(10));

        assinatura.pagaNoPix(TRINTA_DIAS, depoisDoTeste);
        assinatura.pagaNoPix(TRINTA_DIAS, depoisDoTeste);

        assertThat(assinatura.getAcessoAte()).isEqualTo(depoisDoTeste.plus(TRINTA_DIAS).plus(TRINTA_DIAS));
    }

    @Test
    void faturaDoCartaoNuncaDiminuiOAcesso() {
        Assinatura assinatura = nova();
        Instant fimDoMes = CADASTRO.plus(Duration.ofDays(37));

        assinatura.pagaNoCartaoAte(fimDoMes, CADASTRO);
        assinatura.pagaNoCartaoAte(fimDoMes.minus(Duration.ofDays(5)), CADASTRO);

        assertThat(assinatura.getAcessoAte()).isEqualTo(fimDoMes);
        assertThat(assinatura.getFormaPagamento()).isEqualTo(FormaPagamento.CARTAO);
    }

    @Test
    void cancelarNaoTiraOQueJaFoiPago() {
        Assinatura assinatura = nova();
        Instant fimDoMes = CADASTRO.plus(Duration.ofDays(37));
        assinatura.pagaNoCartaoAte(fimDoMes, CADASTRO);

        assinatura.cancelada(CADASTRO.plus(Duration.ofDays(20)));

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.ativa(CADASTRO.plus(Duration.ofDays(30)))).isTrue();
        assertThat(assinatura.ativa(fimDoMes)).isFalse();
    }
}
