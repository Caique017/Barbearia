package com.barbearia.api.service;

import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.exceptions.PagamentoIndisponivelException;
import com.barbearia.api.exceptions.WebhookInvalidoException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class StripeGateway implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

    private final String chaveSecreta;
    private final String segredoWebhook;
    private final String precoCartao;
    private final long centavosPix;
    private final String urlFront;

    public StripeGateway(@Value("${api.stripe.chave-secreta:}") String chaveSecreta,
                         @Value("${api.stripe.webhook-secret:}") String segredoWebhook,
                         @Value("${api.stripe.preco-cartao:}") String precoCartao,
                         @Value("${api.assinatura.preco-centavos:2999}") long centavosPix,
                         @Value("${api.front.url}") String urlFront) {
        this.chaveSecreta = chaveSecreta;
        this.segredoWebhook = segredoWebhook;
        this.precoCartao = precoCartao;
        this.centavosPix = centavosPix;
        this.urlFront = urlFront;
    }

    @Override
    public boolean configurado() {
        return !chaveSecreta.isBlank() && !precoCartao.isBlank();
    }

    @Override
    public String criarCliente(UUID barbeiroId, String nome, String email) {
        try {
            return Customer.create(CustomerCreateParams.builder()
                    .setName(nome)
                    .setEmail(email)
                    .putMetadata("barbeiro_id", barbeiroId.toString())
                    .build(), opcoes("cliente-" + barbeiroId + "-" + Integer.toHexString(Objects.hash(nome, email))))
                    .getId();
        } catch (StripeException e) {
            throw falha("criar o cliente", e);
        }
    }

    @Override
    public String criarCheckout(String clienteId, UUID barbeiroId, FormaPagamento forma) {
        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setCustomer(clienteId)
                .setClientReferenceId(barbeiroId.toString())
                .setLocale(SessionCreateParams.Locale.PT_BR)
                .setSuccessUrl(urlFront + "/barbeiro/plano?pagamento=ok")
                .setCancelUrl(urlFront + "/barbeiro/plano");

        if (forma == FormaPagamento.CARTAO) {
            params.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(precoCartao)
                            .setQuantity(1L)
                            .build());
        } else {
            params.setMode(SessionCreateParams.Mode.PAYMENT)
                    .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.PIX)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("brl")
                                    .setUnitAmount(centavosPix)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("ClipBook - 30 dias de acesso")
                                            .build())
                                    .build())
                            .build());
        }

        try {
            return Session.create(params.build(), opcoes()).getUrl();
        } catch (StripeException e) {
            throw falha("abrir o pagamento", e);
        }
    }

    @Override
    public String criarPortal(String clienteId) {
        try {
            return com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(clienteId)
                            .setReturnUrl(urlFront + "/barbeiro/plano")
                            .build(), opcoes()).getUrl();
        } catch (StripeException e) {
            throw falha("abrir o portal de pagamento", e);
        }
    }

    @Override
    public Evento lerEvento(String payload, String assinatura) {
        if (segredoWebhook.isBlank()) {
            throw new PagamentoIndisponivelException("Pagamentos ainda não configurados.");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(payload, assinatura, segredoWebhook);
        } catch (SignatureVerificationException | RuntimeException e) {
            throw new WebhookInvalidoException("Assinatura do webhook inválida.");
        }

        StripeObject objeto = objeto(evento);
        return switch (evento.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                Session sessao = (Session) objeto;
                boolean assinaturaNoCartao = "subscription".equals(sessao.getMode());
                yield new Evento(evento.getId(), TipoEvento.CHECKOUT_CONCLUIDO, sessao.getCustomer(),
                        sessao.getSubscription(), assinaturaNoCartao ? FormaPagamento.CARTAO : FormaPagamento.PIX,
                        "paid".equals(sessao.getPaymentStatus()), null, false);
            }
            case "invoice.paid" -> {
                Invoice fatura = (Invoice) objeto;
                Instant ate = fatura.getLines().getData().stream()
                        .map(linha -> Instant.ofEpochSecond(linha.getPeriod().getEnd()))
                        .max(Instant::compareTo)
                        .orElse(null);
                yield new Evento(evento.getId(), TipoEvento.FATURA_PAGA, fatura.getCustomer(), null,
                        FormaPagamento.CARTAO, true, ate, false);
            }
            case "invoice.payment_failed" -> new Evento(evento.getId(), TipoEvento.FATURA_FALHOU,
                    ((Invoice) objeto).getCustomer(), null, FormaPagamento.CARTAO, false, null, false);
            case "customer.subscription.updated" -> {
                Subscription atualizada = (Subscription) objeto;
                boolean agendado = Boolean.TRUE.equals(atualizada.getCancelAtPeriodEnd())
                        || atualizada.getCancelAt() != null;
                yield new Evento(evento.getId(), TipoEvento.ASSINATURA_ATUALIZADA, atualizada.getCustomer(),
                        atualizada.getId(), FormaPagamento.CARTAO, false, null, agendado);
            }
            case "customer.subscription.deleted" -> {
                Subscription cancelada = (Subscription) objeto;
                yield new Evento(evento.getId(), TipoEvento.ASSINATURA_CANCELADA, cancelada.getCustomer(),
                        cancelada.getId(), FormaPagamento.CARTAO, false, null, false);
            }
            default -> new Evento(evento.getId(), TipoEvento.IGNORADO, null, null, null, false, null, false);
        };
    }

    private static StripeObject objeto(Event evento) {
        EventDataObjectDeserializer deserializador = evento.getDataObjectDeserializer();
        return deserializador.getObject().orElseGet(() -> {
            try {
                return deserializador.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new WebhookInvalidoException("Evento do Stripe em formato inesperado.");
            }
        });
    }

    private RequestOptions opcoes() {
        return opcoes(null);
    }

    private RequestOptions opcoes(String chaveIdempotencia) {
        if (!configurado()) {
            throw new PagamentoIndisponivelException("Pagamentos ainda não configurados.");
        }
        return RequestOptions.builder().setApiKey(chaveSecreta).setIdempotencyKey(chaveIdempotencia).build();
    }

    private static PagamentoIndisponivelException falha(String acao, StripeException e) {
        log.error("Stripe falhou ao {}: {}", acao, e.getMessage());
        return new PagamentoIndisponivelException("Não foi possível " + acao + " agora. Tente de novo em instantes.");
    }
}
