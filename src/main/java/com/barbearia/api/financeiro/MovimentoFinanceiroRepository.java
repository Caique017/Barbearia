package com.barbearia.api.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {
}
