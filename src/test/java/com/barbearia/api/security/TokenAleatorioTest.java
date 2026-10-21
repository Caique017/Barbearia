package com.barbearia.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAleatorioTest {

    @Test
    void doisTokensGeradosSaoDiferentes() {
        assertThat(TokenAleatorio.gerar()).isNotEqualTo(TokenAleatorio.gerar());
    }

    @Test
    void mesmoTokenGeraSempreOMesmoHash() {
        String token = TokenAleatorio.gerar();

        assertThat(TokenAleatorio.hash(token)).isEqualTo(TokenAleatorio.hash(token));
    }

    @Test
    void hashTem64CaracteresHex() {
        assertThat(TokenAleatorio.hash(TokenAleatorio.gerar())).matches("[0-9a-f]{64}");
    }

    @Test
    void hashDeSha256Conhecido() {
        assertThat(TokenAleatorio.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void tokenPodeIrNumaUrl() {
        for (int i = 0; i < 100; i++) {
            assertThat(TokenAleatorio.gerar()).matches("[A-Za-z0-9_-]{43}");
        }
    }
}
