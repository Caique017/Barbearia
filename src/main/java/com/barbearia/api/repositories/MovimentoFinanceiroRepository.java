package com.barbearia.api.repositories;

import com.barbearia.api.domain.MovimentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {
}
