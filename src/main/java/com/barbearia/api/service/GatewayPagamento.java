package com.barbearia.api.service;

import com.barbearia.api.enums.FormaPagamento;

import java.time.Instant;
import java.util.UUID;

public interface GatewayPagamento {

    boolean configurado();

    String criarCliente(UUID barbeiroId, String nome, String email);

    String criarCheckout(String clienteId, UUID barbeiroId, FormaPagamento forma);

    String criarPortal(String clienteId);

    Evento lerEvento(String payload, String assinatura);

    enum TipoEvento {
        CHECKOUT_CONCLUIDO,
        FATURA_PAGA,
        FATURA_FALHOU,
        ASSINATURA_ATUALIZADA,
        ASSINATURA_CANCELADA,
        IGNORADO
    }

    record Evento(
            String id,
            TipoEvento tipo,
            String clienteId,
            String assinaturaId,
            FormaPagamento forma,
            boolean pago,
            Instant periodoAte,
            boolean cancelamentoAgendado
    ) {
    }
}
