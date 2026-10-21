package com.barbearia.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    @Test
    void normalizarDeixaMinusculoESemEspacos() {
        assertThat(Emails.normalizar("  Joao.Silva@Ex.COM ")).isEqualTo("joao.silva@ex.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.com", "joao.silva@ex.com", "x_y+z@dominio.com.br"})
    void emailJaNormalizadoNaoMuda(String email) {
        assertThat(Emails.normalizar(email)).isEqualTo(email);
    }

    @Test
    void normalizarPreservaNull() {
        assertThat(Emails.normalizar(null)).isNull();
    }

    @Test
    void mascararDeixaSoAPrimeiraLetraEODominio() {
        assertThat(Emails.mascarar("joao.silva@ex.com")).isEqualTo("j***@ex.com");
    }

    @Test
    void mascararSemArrobaEscondeTudo() {
        assertThat(Emails.mascarar("lixo")).isEqualTo("***");
        assertThat(Emails.mascarar(null)).isNull();
    }
}
