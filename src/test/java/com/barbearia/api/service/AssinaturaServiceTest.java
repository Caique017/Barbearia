package com.barbearia.api.service;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.EventoPagamentoProcessado;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.enums.StatusAssinatura;
import com.barbearia.api.exceptions.AssinaturaDuplicadaException;
import com.barbearia.api.exceptions.AssinaturaInativaException;
import com.barbearia.api.exceptions.PagamentoIndisponivelException;
import com.barbearia.api.repositories.AssinaturaRepository;
import com.barbearia.api.repositories.EventoPagamentoProcessadoRepository;
import com.barbearia.api.service.GatewayPagamento.Evento;
import com.barbearia.api.service.GatewayPagamento.TipoEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private AssinaturaRepository assinaturaRepository;
    @Mock
    private EventoPagamentoProcessadoRepository eventoRepository;
    @Mock
    private GatewayPagamento gateway;
    @Mock
    private BarbeiroLogado barbeiroLogado;

    private AssinaturaService service;
    private Barbeiro barbeiro;
    private Assinatura assinatura;

    @BeforeEach
    void criar() {
        service = new AssinaturaService(assinaturaRepository, eventoRepository, gateway, barbeiroLogado,
                Clock.fixed(AGORA, ZoneOffset.UTC), 7, 30, 2999);
        barbeiro = Barbeiro.builder().id(UUID.randomUUID()).nomeBarbearia("Barbearia X").build();
        assinatura = Assinatura.emTeste(barbeiro, AGORA.minus(Duration.ofDays(10)), Duration.ofDays(7));
        assinatura.setStripeCustomerId("cus_1");
    }

    private void webhook(Evento evento) {
        when(gateway.lerEvento("payload", "assinatura")).thenReturn(evento);
        service.processarWebhook("payload", "assinatura");
    }

    private void clienteConhecido() {
        when(assinaturaRepository.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(assinatura));
    }

    @Test
    void pixPagoLibera30DiasEGuardaOEvento() {
        clienteConhecido();

        webhook(new Evento("evt_1", TipoEvento.CHECKOUT_CONCLUIDO, "cus_1", null, FormaPagamento.PIX, true,
                null, false));

        assertThat(assinatura.ativa(AGORA)).isTrue();
        assertThat(assinatura.getAcessoAte()).isEqualTo(AGORA.plus(Duration.ofDays(30)));
        verify(eventoRepository).save(any(EventoPagamentoProcessado.class));
    }

    @Test
    void pixAindaNaoPagoNaoLibera() {
        clienteConhecido();

        webhook(new Evento("evt_1", TipoEvento.CHECKOUT_CONCLUIDO, "cus_1", null, FormaPagamento.PIX, false,
                null, false));

        assertThat(assinatura.ativa(AGORA)).isFalse();
    }

    @Test
    void eventoRepetidoNaoEProcessadoDuasVezes() {
        when(eventoRepository.existsById("evt_1")).thenReturn(true);

        webhook(new Evento("evt_1", TipoEvento.CHECKOUT_CONCLUIDO, "cus_1", null, FormaPagamento.PIX, true,
                null, false));

        verify(assinaturaRepository, never()).findByStripeCustomerId(any());
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void faturaPagaDoCartaoLiberaAteOFimDoPeriodo() {
        clienteConhecido();
        Instant fim = AGORA.plus(Duration.ofDays(31));

        webhook(new Evento("evt_2", TipoEvento.FATURA_PAGA, "cus_1", null, FormaPagamento.CARTAO, true, fim, false));

        assertThat(assinatura.getAcessoAte()).isEqualTo(fim);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
    }

    @Test
    void checkoutDoCartaoSoGuardaAAssinaturaEEsperaAFatura() {
        clienteConhecido();

        webhook(new Evento("evt_3", TipoEvento.CHECKOUT_CONCLUIDO, "cus_1", "sub_1", FormaPagamento.CARTAO, true,
                null, false));

        assertThat(assinatura.getStripeSubscriptionId()).isEqualTo("sub_1");
        assertThat(assinatura.ativa(AGORA)).isFalse();
    }

    @Test
    void faturaRecusadaECancelamentoMudamOStatus() {
        clienteConhecido();
        assinatura.setStripeSubscriptionId("sub_1");

        webhook(new Evento("evt_4", TipoEvento.FATURA_FALHOU, "cus_1", null, FormaPagamento.CARTAO, false,
                null, false));
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.PAGAMENTO_PENDENTE);

        webhook(new Evento("evt_5", TipoEvento.ASSINATURA_CANCELADA, "cus_1", "sub_1", FormaPagamento.CARTAO, false,
                null, false));
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.assinaNoCartao()).isFalse();
    }

    @Test
    void cancelamentoAgendadoNoPortalApareceEPodeSerDesfeito() {
        clienteConhecido();
        assinatura.setStripeSubscriptionId("sub_1");

        webhook(new Evento("evt_7", TipoEvento.ASSINATURA_ATUALIZADA, "cus_1", "sub_1", FormaPagamento.CARTAO, false,
                null, true));
        assertThat(assinatura.isCancelamentoAgendado()).isTrue();

        webhook(new Evento("evt_8", TipoEvento.ASSINATURA_ATUALIZADA, "cus_1", "sub_1", FormaPagamento.CARTAO, false,
                null, false));
        assertThat(assinatura.isCancelamentoAgendado()).isFalse();
    }

    @Test
    void eventoDeUmaAssinaturaAntigaNaoMexeNaAtual() {
        clienteConhecido();
        assinatura.setStripeSubscriptionId("sub_novo");

        webhook(new Evento("evt_9", TipoEvento.ASSINATURA_ATUALIZADA, "cus_1", "sub_velho", FormaPagamento.CARTAO,
                false, null, true));
        webhook(new Evento("evt_10", TipoEvento.ASSINATURA_CANCELADA, "cus_1", "sub_velho", FormaPagamento.CARTAO,
                false, null, false));

        assertThat(assinatura.isCancelamentoAgendado()).isFalse();
        assertThat(assinatura.getStripeSubscriptionId()).isEqualTo("sub_novo");
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TESTE);
    }

    @Test
    void quemJaAssinaNoCartaoNaoAbreOutroCheckout() {
        Usuario logado = Usuario.builder().email("dono@ex.com").build();
        assinatura.setStripeSubscriptionId("sub_1");
        when(gateway.configurado()).thenReturn(true);
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
        when(assinaturaRepository.findByBarbeiroId(barbeiro.getId())).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.abrirPagamento(logado, FormaPagamento.PIX))
                .isInstanceOf(AssinaturaDuplicadaException.class)
                .hasMessage(AssinaturaService.JA_ASSINA_NO_CARTAO);
        verify(gateway, never()).criarCheckout(any(), any(), any());
    }

    @Test
    void eventoIgnoradoNemEGravado() {
        webhook(new Evento("evt_6", TipoEvento.IGNORADO, null, null, null, false, null, false));

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void checkoutCriaOClienteNoGatewaySoNaPrimeiraVez() {
        Usuario logado = Usuario.builder().email("dono@ex.com").build();
        assinatura.setStripeCustomerId(null);
        when(gateway.configurado()).thenReturn(true);
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
        when(assinaturaRepository.findByBarbeiroId(barbeiro.getId())).thenReturn(Optional.of(assinatura));
        when(gateway.criarCliente(barbeiro.getId(), "Barbearia X", "dono@ex.com")).thenReturn("cus_novo");
        when(gateway.criarCheckout("cus_novo", barbeiro.getId(), FormaPagamento.PIX)).thenReturn("https://pagar");

        assertThat(service.abrirPagamento(logado, FormaPagamento.PIX)).isEqualTo("https://pagar");
        service.abrirPagamento(logado, FormaPagamento.PIX);

        assertThat(assinatura.getStripeCustomerId()).isEqualTo("cus_novo");
        verify(gateway).criarCliente(any(), any(), any());
    }

    @Test
    void semGatewayConfiguradoDa503() {
        when(gateway.configurado()).thenReturn(false);

        assertThatThrownBy(() -> service.abrirPagamento(new Usuario(), FormaPagamento.CARTAO))
                .isInstanceOf(PagamentoIndisponivelException.class);
    }

    @Test
    void testeVencidoBloqueiaComMensagensDiferentesProBarbeiroEProCliente() {
        when(assinaturaRepository.findByBarbeiroId(barbeiro.getId())).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.exigirAtiva(barbeiro))
                .isInstanceOf(AssinaturaInativaException.class)
                .hasMessage(AssinaturaService.TESTE_ACABOU);
        assertThatThrownBy(() -> service.exigirQueAceiteAgendamentos(barbeiro))
                .isInstanceOf(AssinaturaInativaException.class)
                .hasMessage(AssinaturaService.BARBEARIA_INDISPONIVEL);
    }
}
