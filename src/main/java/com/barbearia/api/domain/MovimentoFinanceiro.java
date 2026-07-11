package com.barbearia.api.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class MovimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true, nullable = false)
    private Agendamento agendamento;
    @Column(nullable = false)
    private BigDecimal valorBruto;
    @Column(nullable = false)
    private BigDecimal custoInsumo;
    @NotNull
    @Column(name = "lucro_liquido", nullable = false, precision = 10, scale = 2)
    private BigDecimal lucroLiquido;
    @Column(nullable = false)
    private LocalDate data;

    @PrePersist
    void prePersist() {
        if (lucroLiquido == null && valorBruto != null && custoInsumo != null) {
            lucroLiquido = valorBruto.subtract(custoInsumo);
        }
    }
}
