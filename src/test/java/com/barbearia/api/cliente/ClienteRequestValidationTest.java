package com.barbearia.api.cliente;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteRequestValidationTest {

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
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void cadastroNormalizaTelefoneEEmail() {
        ClienteCadastroRequest request = new ClienteCadastroRequest(
                "Carlos", "(11) 99999-8888", " Carlos@Ex.com", "senha123", UUID.randomUUID());

        assertThat(request.telefone()).isEqualTo("11999998888");
        assertThat(request.email()).isEqualTo("carlos@ex.com");
        assertThat(camposInvalidos(request)).isEmpty();
    }

    @Test
    void cadastroExigeSenhaDe8Caracteres() {
        ClienteCadastroRequest request = new ClienteCadastroRequest(
                "Carlos", "11999998888", "c@ex.com", "1234567", UUID.randomUUID());

        assertThat(camposInvalidos(request)).containsExactly("senha");
    }

    @Test
    void loginAceitaSenhaAntigaDe6Caracteres() {
        assertThat(camposInvalidos(new ClienteLoginRequest("c@ex.com", "123456"))).isEmpty();
    }

    @Test
    void loginNormalizaEmail() {
        ClienteLoginRequest request = new ClienteLoginRequest(" Carlos@Ex.COM ", "senha123");

        assertThat(request.email()).isEqualTo("carlos@ex.com");
        assertThat(camposInvalidos(request)).isEmpty();
    }

    @Test
    void atualizacaoVaziaNaoTemViolacao() {
        assertThat(camposInvalidos(new ClienteAtualizacaoRequest(null, null))).isEmpty();
    }

    @Test
    void atualizacaoNormalizaTelefone() {
        ClienteAtualizacaoRequest request = new ClienteAtualizacaoRequest(null, "(21) 98765-4321");

        assertThat(request.telefone()).isEqualTo("21987654321");
        assertThat(camposInvalidos(request)).isEmpty();
    }

    @Test
    void atualizacaoRecusaNomeEmBrancoETelefoneInvalido() {
        assertThat(camposInvalidos(new ClienteAtualizacaoRequest(" ", null))).containsExactly("nome");
        assertThat(camposInvalidos(new ClienteAtualizacaoRequest(null, "123"))).containsExactly("telefone");
        assertThat(camposInvalidos(new ClienteAtualizacaoRequest(null, ""))).containsExactly("telefone");
    }
}
