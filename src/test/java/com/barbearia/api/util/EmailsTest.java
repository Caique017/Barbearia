package com.barbearia.api.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    @Test
    void normalizarDeixaMinusculoESemEspacos() {
        assertThat(Emails.normalizar("  Joao.Silva@Ex.COM ")).isEqualTo("joao.silva@ex.com");
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
