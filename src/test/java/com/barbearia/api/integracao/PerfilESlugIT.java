package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilESlugIT extends IntegracaoBase {

    private static final String HORARIO = "Seg a Sex, 9h as 19h";

    @Test
    void horarioGravadoAparecerNaTelaPublica() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/barbeiros/me", barbeiro.token(), null)
                .andExpect(jsonPath("$.horarioFuncionamento").doesNotExist());

        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("horarioFuncionamento", HORARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarioFuncionamento").value(HORARIO));

        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), null, null)
                .andExpect(jsonPath("$.horarioFuncionamento").value(HORARIO));
    }

    @Test
    void patchVazioNaoMudaNada() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro("Barbearia Intacta " + sufixoUnico());
        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("horarioFuncionamento", HORARIO));

        chamar("PATCH", "/barbeiros/me", barbeiro.token(), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarioFuncionamento").value(HORARIO))
                .andExpect(jsonPath("$.slug").value(barbeiro.slug()));
    }

    @Test
    void nomeOuHorarioEmBrancoDa400ENaoGrava() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("horarioFuncionamento", HORARIO));

        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("nomeBarbearia", " "))
                .andExpect(status().isBadRequest());
        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("nomeBarbearia", "!!! ###"))
                .andExpect(status().isBadRequest());
        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("horarioFuncionamento", "   "))
                .andExpect(status().isBadRequest());

        chamar("GET", "/barbeiros/me", barbeiro.token(), null)
                .andExpect(jsonPath("$.horarioFuncionamento").value(HORARIO));
    }

    @Test
    void trocarONomeNaoTrocaOLink() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("PATCH", "/barbeiros/me", barbeiro.token(), json("nomeBarbearia", "Nome Novo"))
                .andExpect(jsonPath("$.nomeBarbearia").value("Nome Novo"))
                .andExpect(jsonPath("$.slug").value(barbeiro.slug()));

        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), null, null)
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    void trocarOSlugMataOLinkVelho() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String novo = "link-novo-" + sufixoUnico();

        chamar("PATCH", "/barbeiros/me/slug", barbeiro.token(), json("slug", novo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(novo));

        chamar("GET", "/barbeiros/publico/" + novo, null, null).andExpect(status().isOk());
        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), null, null).andExpect(status().isNotFound());
    }

    @Test
    void slugEscolhidoNaoSufixaEmSilencio() throws Exception {
        BarbeiroCriado dono = registrarBarbeiro();
        BarbeiroCriado outro = registrarBarbeiro();

        chamar("PATCH", "/barbeiros/me/slug", outro.token(), json("slug", dono.slug()))
                .andExpect(status().isConflict());
        chamar("PATCH", "/barbeiros/me/slug", outro.token(), json("slug", "publico"))
                .andExpect(status().isConflict());
        chamar("PATCH", "/barbeiros/me/slug", outro.token(), json("slug", "Barbearia Do Joao"))
                .andExpect(status().isBadRequest());
        chamar("PATCH", "/barbeiros/me/slug", outro.token(), json("slug", outro.slug()))
                .andExpect(status().isOk());

        chamar("GET", "/barbeiros/me", outro.token(), null)
                .andExpect(jsonPath("$.slug").value(outro.slug()));
    }

    @Test
    void cadastroDerivadoSufixaEmSilencio() throws Exception {
        String nome = "Studio " + sufixoUnico();
        BarbeiroCriado primeiro = registrarBarbeiro(nome);
        BarbeiroCriado segundo = registrarBarbeiro(nome);

        assertThat(segundo.slug()).isEqualTo(primeiro.slug() + "-2");
    }
}
