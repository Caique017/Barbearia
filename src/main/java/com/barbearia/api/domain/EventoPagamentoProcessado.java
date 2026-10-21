package com.barbearia.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evento_pagamento")
public class EventoPagamentoProcessado {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;
}
