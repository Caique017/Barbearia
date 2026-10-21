package com.barbearia.api.integracao;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.repositories.AssinaturaRepository;
import com.stripe.Stripe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssinaturaIT extends IntegracaoBase {

    private static final String SEGREDO_WEBHOOK = "whsec_segredo_de_teste";

    @Autowired
    private AssinaturaRepository assinaturaRepository;

    private Assinatura assinaturaDe(BarbeiroCriado barbeiro) {
        return assinaturaRepository.findByBarbeiroId(UUID.fromString(barbeiro.barbeiroId())).orElseThrow();
    }

    private void vencerOTeste(BarbeiroCriado barbeiro, String clienteStripe) {
        Assinatura assinatura = assinaturaDe(barbeiro);
        assinatura.setTesteAte(Instant.now().minus(Duration.ofDays(1)));
        assinatura.setStripeCustomerId(clienteStripe);
        assinaturaRepository.save(assinatura);
    }

    private static String pixPago(String eventoId, String clienteStripe) {
        return """
                {"id":"%s","object":"event","type":"checkout.session.completed","api_version":"%s",
                 "data":{"object":{"id":"cs_test_1","object":"checkout.session","mode":"payment",
                 "payment_status":"paid","customer":"%s"}}}
                """.formatted(eventoId, Stripe.API_VERSION, clienteStripe);
    }

    private static String assinar(String payload, String segredo) throws Exception {
        long agora = Instant.now().getEpochSecond();
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String assinatura = HexFormat.of().formatHex(
                hmac.doFinal((agora + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + agora + ",v1=" + assinatura;
    }

    private void enviarWebhook(String payload, String segredo, int statusEsperado) throws Exception {
        mvc.perform(post("/pagamentos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", assinar(payload, segredo))
                        .content(payload))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void barbeiroNovoComecaComSeteDiasDeTeste() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/assinatura", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TESTE"))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.diasRestantes").value(6))
                .andExpect(jsonPath("$.precoMensal").value(29.99));
    }

    @Test
    void testeVencidoDeixaSoLerEOClienteNaoAgenda() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        vencerOTeste(barbeiro, "cus_" + sufixoUnico());

        chamar("GET", "/servicos", barbeiro.token(), null).andExpect(status().isOk());
        chamar("POST", "/servicos", barbeiro.token(), "{\"nome\":\"Corte\",\"preco\":40,\"duracaoMinutos\":30}")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.mensagem").value(
                        "Seu período de teste acabou. Assine para continuar usando a agenda."));
        chamar("GET", "/assinatura", barbeiro.token(), null).andExpect(jsonPath("$.ativa").value(false));

        String cliente = ler(registrarCliente(barbeiro.barbeiroId(), "Cliente", telefoneUnico(), emailUnico())
                .andReturn(), "$.token");
        chamar("POST", "/agendamentos", cliente,
                json("servicoId", UUID.randomUUID().toString(), "dataHora", "2030-01-07T10:00:00"))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void pixPagoPeloWebhookReabreAAgendaEEventoRepetidoNaoSomaDeNovo() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String clienteStripe = "cus_" + sufixoUnico();
        vencerOTeste(barbeiro, clienteStripe);
        String payload = pixPago("evt_" + sufixoUnico(), clienteStripe);

        enviarWebhook(payload, SEGREDO_WEBHOOK, 200);
        Instant liberadaAte = assinaturaDe(barbeiro).getAcessoAte();
        enviarWebhook(payload, SEGREDO_WEBHOOK, 200);

        assertThat(assinaturaDe(barbeiro).getAcessoAte()).isEqualTo(liberadaAte);
        assertThat(liberadaAte).isAfter(Instant.now().plus(Duration.ofDays(29)));
        chamar("POST", "/servicos", barbeiro.token(), "{\"nome\":\"Corte\",\"preco\":40,\"duracaoMinutos\":30}")
                .andExpect(status().isCreated());
    }

    @Test
    void webhookComAssinaturaErradaDa400ENaoMexeEmNada() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String clienteStripe = "cus_" + sufixoUnico();
        vencerOTeste(barbeiro, clienteStripe);

        enviarWebhook(pixPago("evt_" + sufixoUnico(), clienteStripe), "whsec_falso", 400);

        assertThat(assinaturaDe(barbeiro).getAcessoAte()).isNull();
    }

    @Test
    void checkoutSemStripeConfiguradoDa503() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("POST", "/assinatura/checkout", barbeiro.token(), json("formaPagamento", "PIX"))
                .andExpect(status().isServiceUnavailable());
    }
}
