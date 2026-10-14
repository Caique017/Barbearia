package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.barbeiro.BarbeiroAtualizacaoRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroPerfilResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroSlugRequest;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.SlugIndisponivelException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarbeiroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private BarbeiroService barbeiroService;

    private Barbeiro registrar(String nome, String nomeBarbearia) {
        barbeiroService.registroBarbeiro(
                new BarbeiroCadastroRequest(nome, nomeBarbearia, null, "a@ex.com", "senha123"));

        ArgumentCaptor<Barbeiro> salvo = ArgumentCaptor.forClass(Barbeiro.class);
        verify(barbeiroRepository).save(salvo.capture());
        return salvo.getValue();
    }

    @ParameterizedTest
    @CsvSource({
            "Barbearia do João, barbearia-do-joao",
            "João Ção, joao-cao",
            "'  Studio   Alfa!! ', studio-alfa",
            "理发店, barbearia",
            "Барбершоп, barbearia"
    })
    void slugNormalizaONomeDaBarbearia(String nomeBarbearia, String slugEsperado) {
        assertThat(registrar("Pessoa Teste", nomeBarbearia).getSlug()).isEqualTo(slugEsperado);
    }

    @Test
    void slugSaiDoNomeDaBarbeariaENaoDoNomeDaPessoa() {
        Barbeiro barbeiro = registrar("Pessoa Teste", "Barbearia Nova");

        assertThat(barbeiro.getSlug()).isEqualTo("barbearia-nova");
        assertThat(barbeiro.getNomeBarbearia()).isEqualTo("Barbearia Nova");
    }

    @Test
    void slugOcupadoRecebeSufixo() {
        when(barbeiroRepository.existsBySlug("studio-alfa")).thenReturn(true);
        when(barbeiroRepository.existsBySlug("studio-alfa-2")).thenReturn(true);

        assertThat(registrar("Pessoa", "Studio Alfa").getSlug()).isEqualTo("studio-alfa-3");
    }

    @Test
    void slugReservadoRecebeSufixoSemConsultarOBanco() {
        assertThat(registrar("Pessoa", "Me").getSlug()).isEqualTo("me-2");
        verify(barbeiroRepository, never()).existsBySlug("me");
    }

    @Test
    void slugLongoETruncadoSemHifenNoFim() {
        String nome = "a".repeat(109) + " " + "b".repeat(20);

        String slug = registrar("Pessoa", nome).getSlug();

        assertThat(slug).hasSize(109).doesNotEndWith("-");
    }

    @Test
    void cadastroDevolveOSlugGerado() {
        BarbeiroCadastroResponse response = barbeiroService.registroBarbeiro(
                new BarbeiroCadastroRequest("Pessoa", "Barbearia Nova", null, "a@ex.com", "senha123"));

        assertThat(response.slug()).isEqualTo("barbearia-nova");
    }

    @Test
    void cadastroComEmailExistenteDa409SemSalvarNada() {
        when(usuarioRepository.findByEmail("a@ex.com")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> barbeiroService.registroBarbeiro(
                new BarbeiroCadastroRequest("Pessoa", "Barbearia", null, "a@ex.com", "senha123")))
                .isInstanceOf(UsuarioExistenteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(barbeiroRepository, never()).save(any());
    }

    private Usuario logadoCom(Barbeiro barbeiro) {
        Usuario usuario = Usuario.builder().nome("Pessoa").email("a@ex.com").role(Role.BARBEIRO).build();
        when(barbeiroRepository.findByUsuario(usuario)).thenReturn(Optional.of(barbeiro));
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
    void atualizarSlugReservadoDa409() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        assertThatThrownBy(() -> barbeiroService.atualizarSlug(logado, new BarbeiroSlugRequest("publico")))
                .isInstanceOf(SlugIndisponivelException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void atualizarSlugDeOutroBarbeiroDa409SemSufixar() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));
        when(barbeiroRepository.existsBySlug("ocupado")).thenReturn(true);

        assertThatThrownBy(() -> barbeiroService.atualizarSlug(logado, new BarbeiroSlugRequest("ocupado")))
                .isInstanceOf(SlugIndisponivelException.class)
                .hasMessageContaining("em uso");
    }

    @Test
    void atualizarSlugParaOProprioEIdempotente() {
        Usuario logado = logadoCom(barbeiro("meu-slug"));

        BarbeiroPerfilResponse perfil = barbeiroService.atualizarSlug(logado, new BarbeiroSlugRequest("meu-slug"));

        assertThat(perfil.slug()).isEqualTo("meu-slug");
        verify(barbeiroRepository, never()).existsBySlug(any());
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
}
