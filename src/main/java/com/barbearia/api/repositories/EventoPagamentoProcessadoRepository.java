package com.barbearia.api.repositories;

import com.barbearia.api.domain.EventoPagamentoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPagamentoProcessadoRepository extends JpaRepository<EventoPagamentoProcessado, String> {
}
