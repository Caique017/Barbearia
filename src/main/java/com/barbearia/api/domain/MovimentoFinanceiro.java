package com.barbearia.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "movimento_financeiro")
public class MovimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true, nullable = false)
    private Agendamento agendamento;

    @Column(name = "valor_bruto", nullable = false)
    private BigDecimal valorBruto;

    @Column(name = "custo_insumo", nullable = false)
    private BigDecimal custoInsumo;

    @Column(name = "lucro_liquido", nullable = false, precision = 10, scale = 2)
    private BigDecimal lucroLiquido;

    @Column(nullable = false)
    private LocalDate data;

    public static MovimentoFinanceiro daConclusao(Agendamento agendamento) {
        return MovimentoFinanceiro.builder()
                .agendamento(agendamento)
                .valorBruto(agendamento.getPreco())
                .custoInsumo(agendamento.getCustoInsumo())
                .lucroLiquido(agendamento.getPreco().subtract(agendamento.getCustoInsumo()))
                .data(agendamento.getDataHora().toLocalDate())
                .build();
    }
}
