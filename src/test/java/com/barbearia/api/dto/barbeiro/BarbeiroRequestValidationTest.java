package com.barbearia.api.dto.barbeiro;

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

class BarbeiroRequestValidationTest {

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

    private static BarbeiroCadastroRequest cadastro(String nomeBarbearia, String horario, String senha) {
        return new BarbeiroCadastroRequest("Pessoa Teste", nomeBarbearia, horario, "a@ex.com", senha);
    }

    private static Set<String> camposInvalidos(Object request) {
        Set<? extends ConstraintViolation<?>> violacoes = validator.validate(request);
        return violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Barbearia do João", "理发店", "Барбершоп", "7", "Me", "João Ção"})
    void nomeBarbeariaComLetraOuNumeroEValido(String nome) {
        assertThat(camposInvalidos(cadastro(nome, null, "senha123"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!! ###", " ", "", "   \n  "})
    void nomeBarbeariaSemLetraNemNumeroEInvalido(String nome) {
        assertThat(camposInvalidos(cadastro(nome, null, "senha123"))).containsExactly("nomeBarbearia");
    }

    @Test
    void nomeBarbeariaAcimaDe120CaracteresEInvalido() {
        assertThat(camposInvalidos(cadastro("a".repeat(121), null, "senha123"))).containsExactly("nomeBarbearia");
    }

    @ParameterizedTest
    @ValueSource(strings = {"24h", "Seg a Sex, 9h às 19h", "Seg a Sex 9h\nSáb 9h"})
    void horarioComConteudoEValido(String horario) {
        assertThat(camposInvalidos(cadastro("Barbearia", horario, "senha123"))).isEmpty();
    }

    @Test
    void horarioNuloEValidoNoCadastro() {
        assertThat(camposInvalidos(cadastro("Barbearia", null, "senha123"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void horarioEmBrancoEInvalido(String horario) {
        assertThat(camposInvalidos(cadastro("Barbearia", horario, "senha123"))).containsExactly("horarioFuncionamento");
    }

    @Test
    void senhaPrecisaDe8A64Caracteres() {
        assertThat(camposInvalidos(cadastro("Barbearia", null, "1234567"))).containsExactly("senha");
        assertThat(camposInvalidos(cadastro("Barbearia", null, "12345678"))).isEmpty();
        assertThat(camposInvalidos(cadastro("Barbearia", null, "a".repeat(64)))).isEmpty();
        assertThat(camposInvalidos(cadastro("Barbearia", null, "a".repeat(65)))).containsExactly("senha");
    }

    @Test
    void senhaDentroDe64CaracteresMasAcimaDe72BytesEInvalida() {
        String senha = "ç".repeat(40);
        assertThat(senha).hasSize(40);
        assertThat(camposInvalidos(cadastro("Barbearia", null, senha))).containsExactly("senha");
    }

    @Test
    void cadastroNormalizaEmail() {
        BarbeiroCadastroRequest request = new BarbeiroCadastroRequest(
                "Pessoa", "Barbearia", null, "  Joao@Ex.COM ", "senha123");
        assertThat(request.email()).isEqualTo("joao@ex.com");
    }

    @Test
    void loginNaoExigeTamanhoMinimoDeSenhaENormalizaEmail() {
        BarbeiroLoginRequest request = new BarbeiroLoginRequest("Joao@Ex.com", "123456");
        assertThat(camposInvalidos(request)).isEmpty();
        assertThat(request.email()).isEqualTo("joao@ex.com");
    }

    @Test
    void atualizacaoVaziaNaoTemViolacao() {
        assertThat(camposInvalidos(new BarbeiroAtualizacaoRequest(null, null))).isEmpty();
    }

    @Test
    void atualizacaoComNomeOuHorarioEmBrancoEInvalida() {
        assertThat(camposInvalidos(new BarbeiroAtualizacaoRequest(" ", null))).containsExactly("nomeBarbearia");
        assertThat(camposInvalidos(new BarbeiroAtualizacaoRequest("!!! ###", null))).containsExactly("nomeBarbearia");
        assertThat(camposInvalidos(new BarbeiroAtualizacaoRequest(null, "   "))).containsExactly("horarioFuncionamento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"barbearia-do-joao", "studio1", "a", "a-1-b"})
    void slugNoFormatoEValido(String slug) {
        assertThat(camposInvalidos(new BarbeiroSlugRequest(slug))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Barbearia Do Joao", "-joao", "joao-", "joao--silva", "joão", ""})
    void slugForaDoFormatoEInvalido(String slug) {
        assertThat(camposInvalidos(new BarbeiroSlugRequest(slug))).containsExactly("slug");
    }
}
