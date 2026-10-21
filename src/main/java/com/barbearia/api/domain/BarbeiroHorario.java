package com.barbearia.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "barbeiro_horario")
public class BarbeiroHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Convert(converter = DiaSemanaConverter.class)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime abre;

    @Column(nullable = false)
    private LocalTime fecha;
}
