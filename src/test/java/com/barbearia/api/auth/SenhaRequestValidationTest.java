package com.barbearia.api.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaRequestValidationTest {

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

    @Test
    void pedidoNormalizaEmail() {
        EsqueciSenhaRequest request = new EsqueciSenhaRequest("  Joao@Ex.COM ");

        assertThat(camposInvalidos(request)).isEmpty();
        assertThat(request.email()).isEqualTo("joao@ex.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sem-arroba", "a@"})
    void pedidoComEmailInvalidoEInvalido(String email) {
        assertThat(camposInvalidos(new EsqueciSenhaRequest(email))).containsExactly("email");
    }

    @Test
    void redefinicaoValida() {
        assertThat(camposInvalidos(new RedefinirSenhaRequest("token", "senha123"))).isEmpty();
    }

    @Test
    void redefinicaoSemTokenEInvalida() {
        assertThat(camposInvalidos(new RedefinirSenhaRequest(" ", "senha123"))).containsExactly("token");
    }

    @Test
    void novaSenhaPrecisaDe8A64Caracteres() {
        assertThat(camposInvalidos(new RedefinirSenhaRequest("token", "1234567"))).containsExactly("novaSenha");
        assertThat(camposInvalidos(new RedefinirSenhaRequest("token", "a".repeat(64)))).isEmpty();
        assertThat(camposInvalidos(new RedefinirSenhaRequest("token", "a".repeat(65)))).containsExactly("novaSenha");
    }

    @Test
    void novaSenhaAcimaDe72BytesEInvalida() {
        assertThat(camposInvalidos(new RedefinirSenhaRequest("token", "ç".repeat(40)))).containsExactly("novaSenha");
    }
}
