package com.barbearia.api.barbeiro;

import com.barbearia.api.auth.Role;
import com.barbearia.api.auth.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarbeiroServiceTest {

    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;
    @Mock
    private GeradorSlug geradorSlug;

    @InjectMocks
    private BarbeiroService barbeiroService;

    private Usuario logadoCom(Barbeiro barbeiro) {
        Usuario usuario = Usuario.builder().nome("Pessoa").email("a@ex.com").role(Role.BARBEIRO).build();
        when(barbeiroLogado.de(usuario)).thenReturn(barbeiro);
        return usuario;
    }

    private static Barbeiro barbeiro(String slug) {
        return Barbeiro.builder()
                .nomeBarbearia("Barbearia Original")
                .horarioFuncionamento("Seg a Sex, 9h às 19h")
                .slug(slug)
                .build();
    }

    @Test
    void atualizarSlugIndisponivelNaoTrocaOSlug() {
        Barbeiro barbeiro = barbeiro("meu-slug");
        Usuario logado = logadoCom(barbeiro);
        doThrow(new SlugIndisponivelException("em uso")).when(geradorSlug).validarEscolhido("ocupado", "meu-slug");

        assertThatThrownBy(() -> barbeiroService.atualizarSlug(logado, new BarbeiroSlugRequest("ocupado")))
                .isInstanceOf(SlugIndisponivelException.class);
        assertThat(barbeiro.getSlug()).isEqualTo("meu-slug");
    }

    @Test
    void atualizarSlugLivreTrocaOSlug() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        BarbeiroPerfilResponse perfil = barbeiroService.atualizarSlug(logado, new BarbeiroSlugRequest("novo-slug"));

        assertThat(perfil.slug()).isEqualTo("novo-slug");
    }

    @Test
    void atualizarPerfilSoComHorarioNaoMexeNoNome() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        BarbeiroPerfilResponse perfil = barbeiroService.atualizarPerfil(logado,
                new BarbeiroAtualizacaoRequest(null, "Ter a Dom, 10h às 20h"));

        assertThat(perfil.horarioFuncionamento()).isEqualTo("Ter a Dom, 10h às 20h");
        assertThat(perfil.nomeBarbearia()).isEqualTo("Barbearia Original");
    }

    @Test
    void atualizarPerfilVazioNaoMudaNada() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        BarbeiroPerfilResponse perfil = barbeiroService.atualizarPerfil(logado, new BarbeiroAtualizacaoRequest(null, null));

        assertThat(perfil.nomeBarbearia()).isEqualTo("Barbearia Original");
        assertThat(perfil.horarioFuncionamento()).isEqualTo("Seg a Sex, 9h às 19h");
    }

    @Test
    void atualizarNomeNaoRegeraOSlug() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        BarbeiroPerfilResponse perfil = barbeiroService.atualizarPerfil(logado,
                new BarbeiroAtualizacaoRequest("Nome Novo", null));

        assertThat(perfil.nomeBarbearia()).isEqualTo("Nome Novo");
        assertThat(perfil.slug()).isEqualTo("meu-slug");
    }

    @Test
    void publicoComSlugInexistenteDa404() {
        when(barbeiroRepository.findBySlug("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barbeiroService.buscarPublico("nao-existe"))
                .isInstanceOf(BarbeiroNaoEncontradoException.class);
    }
}
