package com.barbearia.api.service;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.repositories.AssinaturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class LembreteRenovacaoPix {

    static final Duration ANTECEDENCIA = Duration.ofDays(3);
    private static final Logger log = LoggerFactory.getLogger(LembreteRenovacaoPix.class);

    private final AssinaturaRepository assinaturaRepository;
    private final EmailService emailService;
    private final Clock clock;
    private final String urlFront;

    public LembreteRenovacaoPix(AssinaturaRepository assinaturaRepository, EmailService emailService, Clock clock,
                                @Value("${api.front.url}") String urlFront) {
        this.assinaturaRepository = assinaturaRepository;
        this.emailService = emailService;
        this.clock = clock;
        this.urlFront = urlFront;
    }

    @Scheduled(cron = "${api.assinatura.lembrete-cron:0 0 12 * * *}")
    @Transactional
    public int enviar() {
        Instant agora = clock.instant();
        List<Assinatura> vencendo = assinaturaRepository.vencendoSemLembrete(FormaPagamento.PIX, agora,
                agora.plus(ANTECEDENCIA));

        for (Assinatura assinatura : vencendo) {
            emailService.enviarLembreteRenovacao(assinatura.getBarbeiro().getUsuario().getEmail(),
                    assinatura.getBarbeiro().getUsuario().getNome(), urlFront + "/barbeiro/plano");
            assinatura.setLembreteEnviadoEm(agora);
        }

        log.info("Lembretes de renovação no Pix enviados: {}", vencendo.size());
        return vencendo.size();
    }
}
