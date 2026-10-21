package com.barbearia.api.agendamento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {
}
