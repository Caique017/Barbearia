package com.barbearia.api.compartilhado.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TelefoneTest {

    @Test
    void normalizarRemoveTudoQueNaoEDigito() {
        assertThat(Telefone.normalizar("(11) 99999-8888")).isEqualTo("11999998888");
        assertThat(Telefone.normalizar(" 11 3333.4444 ")).isEqualTo("1133334444");
    }

    @Test
    void normalizarPreservaNull() {
        assertThat(Telefone.normalizar(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11999998888", "1133334444", "21987654321"})
    void padraoAceitaFixoECelular(String telefone) {
        assertThat(telefone).matches(Telefone.PADRAO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01999998888", "11888887777", "119999988", "119999988889", ""})
    void padraoRecusaDddZeroCelularSemNoveETamanhoErrado(String telefone) {
        assertThat(telefone).doesNotMatch(Telefone.PADRAO);
    }
}
