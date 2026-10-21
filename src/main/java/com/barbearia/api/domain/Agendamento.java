package com.barbearia.api.domain;

import com.barbearia.api.enums.StatusAgendamento;
import com.barbearia.api.exceptions.TransicaoAgendamentoInvalidaException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remarcado_para_id")
    private Agendamento remarcadoPara;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "termina_em", nullable = false)
    private LocalDateTime terminaEm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "custo_insumo", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoInsumo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public static Agendamento novo(Barbeiro barbeiro, Cliente cliente, Servico servico, LocalDateTime inicio) {
        return Agendamento.builder()
                .barbeiro(barbeiro)
                .cliente(cliente)
                .servico(servico)
                .dataHora(inicio)
                .terminaEm(inicio.plusMinutes(servico.getDuracaoMinutos()))
                .preco(servico.getPreco())
                .custoInsumo(servico.getCustoInsumo())
                .build();
    }

    public Agendamento copiaEm(LocalDateTime novoInicio) {
        return Agendamento.builder()
                .barbeiro(barbeiro)
                .cliente(cliente)
                .servico(servico)
                .dataHora(novoInicio)
                .terminaEm(novoInicio.plus(duracao()))
                .preco(preco)
                .custoInsumo(custoInsumo)
                .build();
    }

    public Duration duracao() {
        return Duration.between(dataHora, terminaEm);
    }

    public boolean jaComecou(LocalDateTime agora) {
        return !dataHora.isAfter(agora);
    }

    public void concluir(LocalDateTime agora) {
        exigirConfirmado();
        if (!jaComecou(agora)) {
            throw new TransicaoAgendamentoInvalidaException("Só dá para concluir depois do horário marcado.");
        }
        status = StatusAgendamento.CONCLUIDO;
    }

    public void marcarFalta(LocalDateTime agora) {
        exigirConfirmado();
        if (!jaComecou(agora)) {
            throw new TransicaoAgendamentoInvalidaException("Só dá para marcar falta depois do horário marcado.");
        }
        status = StatusAgendamento.NO_SHOW;
    }

    public void cancelar() {
        exigirConfirmado();
        status = StatusAgendamento.CANCELADO;
    }

    public void liberarParaRemarcar() {
        exigirConfirmado();
        status = StatusAgendamento.REMARCADO;
    }

    private void exigirConfirmado() {
        if (status != StatusAgendamento.CONFIRMADO) {
            throw new TransicaoAgendamentoInvalidaException(
                    "Esse agendamento já está " + status.name().toLowerCase() + " e não pode mais ser alterado.");
        }
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
