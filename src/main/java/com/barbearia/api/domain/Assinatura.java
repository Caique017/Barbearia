package com.barbearia.api.domain;

import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.enums.StatusAssinatura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assinatura")
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false, unique = true)
    private Barbeiro barbeiro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAssinatura status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 10)
    private FormaPagamento formaPagamento;

    @Column(name = "teste_ate", nullable = false)
    private Instant testeAte;

    @Column(name = "acesso_ate")
    private Instant acessoAte;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "cancelamento_agendado", nullable = false)
    private boolean cancelamentoAgendado;

    @Column(name = "lembrete_enviado_em")
    private Instant lembreteEnviadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public static Assinatura emTeste(Barbeiro barbeiro, Instant agora, Duration duracaoDoTeste) {
        return Assinatura.builder()
                .barbeiro(barbeiro)
                .status(StatusAssinatura.TESTE)
                .testeAte(agora.plus(duracaoDoTeste))
                .atualizadoEm(agora)
                .build();
    }

    public boolean ativa(Instant agora) {
        return agora.isBefore(testeAte) || (acessoAte != null && agora.isBefore(acessoAte));
    }

    public Instant liberadaAte() {
        return acessoAte != null && acessoAte.isAfter(testeAte) ? acessoAte : testeAte;
    }

    public void pagaNoCartaoAte(Instant fimDoPeriodo, Instant agora) {
        if (acessoAte == null || fimDoPeriodo.isAfter(acessoAte)) {
            acessoAte = fimDoPeriodo;
        }
        formaPagamento = FormaPagamento.CARTAO;
        status = StatusAssinatura.ATIVA;
        atualizadoEm = agora;
    }

    public void pagaNoPix(Duration periodo, Instant agora) {
        Instant base = liberadaAte().isAfter(agora) ? liberadaAte() : agora;
        acessoAte = base.plus(periodo);
        formaPagamento = FormaPagamento.PIX;
        status = StatusAssinatura.ATIVA;
        lembreteEnviadoEm = null;
        atualizadoEm = agora;
    }

    public boolean assinaNoCartao() {
        return stripeSubscriptionId != null;
    }

    public void assinadaNoCartao(String assinaturaId, Instant agora) {
        stripeSubscriptionId = assinaturaId;
        cancelamentoAgendado = false;
        atualizadoEm = agora;
    }

    public void pagamentoFalhou(Instant agora) {
        status = StatusAssinatura.PAGAMENTO_PENDENTE;
        atualizadoEm = agora;
    }

    public void cancelamentoAgendado(String assinaturaId, boolean agendado, Instant agora) {
        if (!assinaturaAtual(assinaturaId)) {
            return;
        }
        cancelamentoAgendado = agendado;
        atualizadoEm = agora;
    }

    public void cancelada(String assinaturaId, Instant agora) {
        if (!assinaturaAtual(assinaturaId)) {
            return;
        }
        status = StatusAssinatura.CANCELADA;
        stripeSubscriptionId = null;
        cancelamentoAgendado = false;
        atualizadoEm = agora;
    }

    private boolean assinaturaAtual(String assinaturaId) {
        return assinaturaId != null && assinaturaId.equals(stripeSubscriptionId);
    }
}
