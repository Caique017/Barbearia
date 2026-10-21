package com.barbearia.api.service;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.EventoPagamentoProcessado;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.assinatura.AssinaturaResponse;
import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.exceptions.AssinaturaInativaException;
import com.barbearia.api.exceptions.PagamentoIndisponivelException;
import com.barbearia.api.repositories.AssinaturaRepository;
import com.barbearia.api.repositories.EventoPagamentoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AssinaturaService {

    static final String TESTE_ACABOU = "Seu período de teste acabou. Assine para continuar usando a agenda.";
    static final String BARBEARIA_INDISPONIVEL = "Essa barbearia não está aceitando agendamentos online no momento.";
    private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);

    private final AssinaturaRepository assinaturaRepository;
    private final EventoPagamentoProcessadoRepository eventoRepository;
    private final GatewayPagamento gateway;
    private final BarbeiroLogado barbeiroLogado;
    private final Clock clock;
    private final Duration duracaoDoTeste;
    private final Duration periodoPix;
    private final BigDecimal precoMensal;

    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             EventoPagamentoProcessadoRepository eventoRepository,
                             GatewayPagamento gateway,
                             BarbeiroLogado barbeiroLogado,
                             Clock clock,
                             @Value("${api.assinatura.dias-teste:7}") int diasDeTeste,
                             @Value("${api.assinatura.dias-pix:30}") int diasPorPix,
                             @Value("${api.assinatura.preco-centavos:2999}") long precoCentavos) {
        this.assinaturaRepository = assinaturaRepository;
        this.eventoRepository = eventoRepository;
        this.gateway = gateway;
        this.barbeiroLogado = barbeiroLogado;
        this.clock = clock;
        this.duracaoDoTeste = Duration.ofDays(diasDeTeste);
        this.periodoPix = Duration.ofDays(diasPorPix);
        this.precoMensal = BigDecimal.valueOf(precoCentavos, 2);
    }

    @Transactional
    public void iniciarTeste(Barbeiro barbeiro) {
        assinaturaRepository.save(Assinatura.emTeste(barbeiro, clock.instant(), duracaoDoTeste));
    }

    @Transactional(readOnly = true)
    public AssinaturaResponse consultar(Usuario logado) {
        return resposta(daBarbearia(barbeiroLogado.de(logado)));
    }

    @Transactional
    public String abrirPagamento(Usuario logado, FormaPagamento forma) {
        exigirGateway();
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        Assinatura assinatura = daBarbearia(barbeiro);

        if (assinatura.getStripeCustomerId() == null) {
            assinatura.setStripeCustomerId(gateway.criarCliente(barbeiro.getId(), barbeiro.getNomeBarbearia(),
                    logado.getEmail()));
        }

        return gateway.criarCheckout(assinatura.getStripeCustomerId(), barbeiro.getId(), forma);
    }

    @Transactional(readOnly = true)
    public String abrirPortal(Usuario logado) {
        exigirGateway();
        Assinatura assinatura = daBarbearia(barbeiroLogado.de(logado));

        if (assinatura.getStripeCustomerId() == null) {
            throw new PagamentoIndisponivelException("Você ainda não tem pagamentos para gerenciar.");
        }
        return gateway.criarPortal(assinatura.getStripeCustomerId());
    }

    @Transactional
    public void processarWebhook(String payload, String assinaturaDoStripe) {
        GatewayPagamento.Evento evento = gateway.lerEvento(payload, assinaturaDoStripe);

        if (evento.tipo() == GatewayPagamento.TipoEvento.IGNORADO || eventoRepository.existsById(evento.id())) {
            return;
        }

        Instant agora = clock.instant();
        Optional<Assinatura> encontrada = Optional.ofNullable(evento.clienteId())
                .flatMap(assinaturaRepository::findByStripeCustomerId);

        if (encontrada.isEmpty()) {
            log.warn("Evento de pagamento {} sem assinatura para o cliente {}", evento.id(), evento.clienteId());
        } else {
            aplicar(encontrada.get(), evento, agora);
        }

        eventoRepository.save(new EventoPagamentoProcessado(evento.id(), evento.tipo().name(), agora));
    }

    @Transactional(readOnly = true)
    public boolean ativa(Barbeiro barbeiro) {
        return assinaturaRepository.findByBarbeiroId(barbeiro.getId())
                .map(assinatura -> assinatura.ativa(clock.instant()))
                .orElse(false);
    }

    public void exigirAtiva(Barbeiro barbeiro) {
        if (!ativa(barbeiro)) {
            throw new AssinaturaInativaException(TESTE_ACABOU);
        }
    }

    public void exigirQueAceiteAgendamentos(Barbeiro barbeiro) {
        if (!ativa(barbeiro)) {
            throw new AssinaturaInativaException(BARBEARIA_INDISPONIVEL);
        }
    }

    private void aplicar(Assinatura assinatura, GatewayPagamento.Evento evento, Instant agora) {
        switch (evento.tipo()) {
            case CHECKOUT_CONCLUIDO -> {
                if (evento.forma() == FormaPagamento.CARTAO) {
                    assinatura.setStripeSubscriptionId(evento.assinaturaId());
                } else if (evento.pago()) {
                    assinatura.pagaNoPix(periodoPix, agora);
                }
            }
            case FATURA_PAGA -> {
                if (evento.periodoAte() != null) {
                    assinatura.pagaNoCartaoAte(evento.periodoAte(), agora);
                }
            }
            case FATURA_FALHOU -> assinatura.pagamentoFalhou(agora);
            case ASSINATURA_CANCELADA -> assinatura.cancelada(agora);
            default -> {
            }
        }
    }

    private Assinatura daBarbearia(Barbeiro barbeiro) {
        return assinaturaRepository.findByBarbeiroId(barbeiro.getId())
                .orElseThrow(() -> new AssinaturaInativaException(TESTE_ACABOU));
    }

    private void exigirGateway() {
        if (!gateway.configurado()) {
            throw new PagamentoIndisponivelException("Pagamentos ainda não configurados.");
        }
    }

    private AssinaturaResponse resposta(Assinatura assinatura) {
        Instant agora = clock.instant();
        Instant ate = assinatura.liberadaAte();
        long dias = ate.isAfter(agora) ? Duration.between(agora, ate).toDays() : 0;

        return new AssinaturaResponse(assinatura.getStatus(), assinatura.ativa(agora), assinatura.getTesteAte(), ate,
                dias, assinatura.getFormaPagamento(), precoMensal);
    }
}
