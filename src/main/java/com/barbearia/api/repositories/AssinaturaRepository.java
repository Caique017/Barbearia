package com.barbearia.api.repositories;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.enums.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    Optional<Assinatura> findByBarbeiroId(UUID barbeiroId);

    Optional<Assinatura> findByStripeCustomerId(String stripeCustomerId);

    @Query("""
            select a from Assinatura a join fetch a.barbeiro b join fetch b.usuario
            where a.formaPagamento = :forma and a.acessoAte between :de and :ate and a.lembreteEnviadoEm is null
            """)
    List<Assinatura> vencendoSemLembrete(FormaPagamento forma, Instant de, Instant ate);
}
