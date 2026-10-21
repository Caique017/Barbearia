package com.barbearia.api.service;

import com.barbearia.api.exceptions.SlugIndisponivelException;
import com.barbearia.api.repositories.BarbeiroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeradorSlugTest {

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @InjectMocks
    private GeradorSlug geradorSlug;

    @ParameterizedTest
    @CsvSource({
            "Barbearia do João, barbearia-do-joao",
            "João Ção, joao-cao",
            "'  Studio   Alfa!! ', studio-alfa",
            "理发店, barbearia",
            "Барбершоп, barbearia"
    })
    void normalizaONomeDaBarbearia(String nomeBarbearia, String slugEsperado) {
        assertThat(geradorSlug.gerar(nomeBarbearia)).isEqualTo(slugEsperado);
    }

    @Test
    void slugOcupadoRecebeSufixo() {
        when(barbeiroRepository.existsBySlug("studio-alfa")).thenReturn(true);
        when(barbeiroRepository.existsBySlug("studio-alfa-2")).thenReturn(true);

        assertThat(geradorSlug.gerar("Studio Alfa")).isEqualTo("studio-alfa-3");
    }

    @Test
    void slugReservadoRecebeSufixoSemConsultarOBanco() {
        assertThat(geradorSlug.gerar("Me")).isEqualTo("me-2");
        verify(barbeiroRepository, never()).existsBySlug("me");
    }

    @Test
    void slugLongoETruncadoSemHifenNoFim() {
        String nome = "a".repeat(109) + " " + "b".repeat(20);

        assertThat(geradorSlug.gerar(nome)).hasSize(109).doesNotEndWith("-");
    }

    @Test
    void escolherSlugReservadoDa409() {
        assertThatThrownBy(() -> geradorSlug.validarEscolhido("publico", "meu-slug"))
                .isInstanceOf(SlugIndisponivelException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void escolherSlugDeOutroBarbeiroDa409SemSufixar() {
        when(barbeiroRepository.existsBySlug("ocupado")).thenReturn(true);

        assertThatThrownBy(() -> geradorSlug.validarEscolhido("ocupado", "meu-slug"))
                .isInstanceOf(SlugIndisponivelException.class)
                .hasMessageContaining("em uso");
    }

    @Test
    void escolherOProprioSlugNaoConsultaOBanco() {
        assertThatCode(() -> geradorSlug.validarEscolhido("meu-slug", "meu-slug")).doesNotThrowAnyException();
        verify(barbeiroRepository, never()).existsBySlug(any());
    }
}
