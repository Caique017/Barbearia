package com.barbearia.api.security;

import com.barbearia.api.exceptions.TentativasExcedidasException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteTentativasLogin {

    static final int MAXIMO_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMITE_PARA_LIMPEZA = 10_000;

    private final Map<Chave, Registro> falhas = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteTentativasLogin() {
        this(Clock.systemUTC());
    }

    LimiteTentativasLogin(Clock clock) {
        this.clock = clock;
    }

    public void verificar(String email, String ip) {
        Instant agora = clock.instant();
        Registro registro = falhas.get(new Chave(email, ip));

        if (registro != null && !registro.expirado(agora) && registro.quantidade() >= MAXIMO_FALHAS) {
            long segundos = Duration.between(agora, registro.inicio().plus(JANELA)).toSeconds();
            long minutos = Math.max(1, (segundos + 59) / 60);
            throw new TentativasExcedidasException(
                    "Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s).", segundos);
        }
    }

    public void registrarFalha(String email, String ip) {
        Instant agora = clock.instant();
        falhas.compute(new Chave(email, ip), (chave, atual) -> atual == null || atual.expirado(agora)
                ? new Registro(1, agora)
                : new Registro(atual.quantidade() + 1, atual.inicio()));

        if (falhas.size() > LIMITE_PARA_LIMPEZA) {
            falhas.values().removeIf(registro -> registro.expirado(agora));
        }
    }

    public void limpar(String email) {
        falhas.keySet().removeIf(chave -> chave.email().equals(email));
    }

    private record Chave(String email, String ip) {
    }

    private record Registro(int quantidade, Instant inicio) {

        boolean expirado(Instant agora) {
            return !agora.isBefore(inicio.plus(JANELA));
        }
    }
}
