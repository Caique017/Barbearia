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
}
