package com.barbearia.api.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false)
    private BigDecimal preco;
    @Column(nullable = false)
    private Integer duracaoMinutos;
    @Column(nullable = false)
    private BigDecimal custoInsumo = BigDecimal.ZERO;
    private boolean ativo = true;
}
