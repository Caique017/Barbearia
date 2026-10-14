package com.barbearia.api.security;

import com.barbearia.api.exceptions.TentativasExcedidasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LimiteTentativasLoginTest {

    private static final String EMAIL = "alvo@ex.com";

    private final RelogioManual relogio = new RelogioManual();
    private final LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);

    private void falhar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.registrarFalha(EMAIL);
        }
    }

    @Test
    void abaixoDoLimiteLiberado() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS - 1);

        assertThatCode(() -> limite.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void noLimiteBloqueiaEInformaQuandoLibera() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS);

        TentativasExcedidasException ex = catchThrowableOfType(TentativasExcedidasException.class,
                () -> limite.verificar(EMAIL));

        assertThat(ex).isNotNull();
        assertThat(ex.getSegundosParaLiberar()).isEqualTo(LimiteTentativasLogin.JANELA.toSeconds());
        assertThat(ex.getMessage()).contains("15 minuto");
    }

    @Test
    void liberaQuandoAJanelaPassa() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS);

        relogio.avancar(LimiteTentativasLogin.JANELA);

        assertThatCode(() -> limite.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void falhaDepoisDaJanelaRecomecaAContagem() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS - 1);
        relogio.avancar(LimiteTentativasLogin.JANELA);

        falhar(1);

        assertThatCode(() -> limite.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void limparZeraAsFalhas() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS);

        limite.limpar(EMAIL);

        assertThatCode(() -> limite.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void bloqueioDeUmEmailNaoAfetaOutro() {
        falhar(LimiteTentativasLogin.MAXIMO_FALHAS);

        assertThatThrownBy(() -> limite.verificar(EMAIL)).isInstanceOf(TentativasExcedidasException.class);
        assertThatCode(() -> limite.verificar("outro@ex.com")).doesNotThrowAnyException();
    }

    private static final class RelogioManual extends Clock {

        private Instant agora = Instant.parse("2026-10-07T12:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
