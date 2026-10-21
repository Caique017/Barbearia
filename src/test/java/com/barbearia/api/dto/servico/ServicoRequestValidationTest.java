package com.barbearia.api.dto.servico;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    private static Set<String> camposInvalidos(Object request) {
        Set<? extends ConstraintViolation<?>> violacoes = validator.validate(request);
        return violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ServicoRequest servico(String nome, String preco, Integer duracao) {
        return new ServicoRequest(nome, preco == null ? null : new BigDecimal(preco), duracao, null);
    }

    @Test
    void servicoValidoSemCusto() {
        ServicoRequest request = servico("  Corte  ", "40.00", 30);

        assertThat(camposInvalidos(request)).isEmpty();
        assertThat(request.nome()).isEqualTo("Corte");
        assertThat(request.custoInsumo()).isEqualByComparingTo("0");
    }

    @Test
    void precoNegativoOuComTresCasasEInvalido() {
        assertThat(camposInvalidos(servico("Corte", "-1.00", 30))).containsExactly("preco");
        assertThat(camposInvalidos(servico("Corte", "10.999", 30))).containsExactly("preco");
    }

    @Test
    void duracaoFicaEntre5MinutosE8Horas() {
        assertThat(camposInvalidos(servico("Corte", "40.00", 4))).containsExactly("duracaoMinutos");
        assertThat(camposInvalidos(servico("Corte", "40.00", 481))).containsExactly("duracaoMinutos");
        assertThat(camposInvalidos(servico("Corte", "40.00", 5))).isEmpty();
    }

    @Test
    void camposObrigatorios() {
        assertThat(camposInvalidos(servico(" ", null, null)))
                .containsExactlyInAnyOrder("nome", "preco", "duracaoMinutos");
    }

    @Test
    void atualizacaoVaziaEValida() {
        assertThat(camposInvalidos(new ServicoAtualizacaoRequest(null, null, null, null, null))).isEmpty();
    }
}
