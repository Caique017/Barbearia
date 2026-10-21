package com.barbearia.api.service;

import com.barbearia.api.domain.TokenRedefinicaoSenha;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.TokenRedefinicaoInvalidoException;
import com.barbearia.api.repositories.TokenRedefinicaoSenhaRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.EventosSeguranca;
import com.barbearia.api.security.LimiteTentativasLogin;
import com.barbearia.api.security.TokenAleatorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedefinicaoSenhaServiceTest {

    private static final String EMAIL = "pessoa@ex.com";
    private static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");
    private static final String FRONT = "http://front.test";
    private static final String PREFIXO_LINK = FRONT + "/redefinir-senha?token=";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenRedefinicaoSenhaRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LimiteTentativasLogin limiteTentativas;
    @Mock
    private EventosSeguranca eventos;

    private RedefinicaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void criarService() {
        service = new RedefinicaoSenhaService(usuarioRepository, tokenRepository, passwordEncoder, eventPublisher,
                limiteTentativas, eventos, Clock.fixed(AGORA, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "frontUrl", FRONT);
        ReflectionTestUtils.setField(service, "expiracaoMinutos", 30L);
        usuario = Usuario.builder().nome("Pessoa").email(EMAIL).senha("hash-velho").role(Role.CLIENTE).build();
    }

    private TokenRedefinicaoSenha token(Instant expiraEm, Instant usadoEm) {
        return TokenRedefinicaoSenha.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .tokenHash(TokenAleatorio.hash("token-do-link"))
                .expiraEm(expiraEm)
                .usadoEm(usadoEm)
                .criadoEm(AGORA.minus(Duration.ofMinutes(10)))
                .build();
    }

    private TokenRedefinicaoSenha tokenValidoNoBanco() {
        TokenRedefinicaoSenha registro = token(AGORA.plus(Duration.ofMinutes(20)), null);
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link"))).thenReturn(Optional.of(registro));
        when(tokenRepository.marcarUsado(registro.getId(), AGORA)).thenReturn(1);
        return registro;
    }

    private RedefinicaoSenhaSolicitada eventoPublicado() {
        ArgumentCaptor<RedefinicaoSenhaSolicitada> evento = ArgumentCaptor.forClass(RedefinicaoSenhaSolicitada.class);
        verify(eventPublisher).publishEvent(evento.capture());
        return evento.getValue();
    }

    @Test
    void solicitarComEmailSemContaNaoFazNada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.solicitar(EMAIL);

        verifyNoInteractions(tokenRepository, eventPublisher);
    }

    @Test
    void solicitarDeNovoEmMenosDe2MinutosNaoFazNada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.existsByUsuarioAndCriadoEmAfter(usuario, AGORA.minus(Duration.ofMinutes(2))))
                .thenReturn(true);

        service.solicitar(EMAIL);

        verify(tokenRepository, never()).save(any());
        verify(tokenRepository, never()).invalidarPendentes(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void solicitarInvalidaOsPendentesAntesDeCriarONovo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        service.solicitar(EMAIL);

        verify(tokenRepository).invalidarPendentes(usuario, AGORA);
    }

    @Test
    void solicitarSalvaTokenQueVenceEm30Minutos() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        ArgumentCaptor<TokenRedefinicaoSenha> salvo = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);

        service.solicitar(EMAIL);

        verify(tokenRepository).save(salvo.capture());
        assertThat(salvo.getValue().getUsuario()).isSameAs(usuario);
        assertThat(salvo.getValue().getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(30)));
        assertThat(salvo.getValue().getCriadoEm()).isEqualTo(AGORA);
        assertThat(salvo.getValue().getUsadoEm()).isNull();
    }

    @Test
    void bancoGuardaOHashEOEventoLevaOTokenPuro() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        ArgumentCaptor<TokenRedefinicaoSenha> salvo = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);

        service.solicitar(EMAIL);

        verify(tokenRepository).save(salvo.capture());
        RedefinicaoSenhaSolicitada evento = eventoPublicado();
        assertThat(evento.email()).isEqualTo(EMAIL);
        assertThat(evento.nome()).isEqualTo("Pessoa");
        assertThat(evento.link()).startsWith(PREFIXO_LINK);

        String tokenDoLink = evento.link().substring(PREFIXO_LINK.length());
        assertThat(salvo.getValue().getTokenHash()).isNotEqualTo(tokenDoLink);
        assertThat(salvo.getValue().getTokenHash()).isEqualTo(TokenAleatorio.hash(tokenDoLink));
        verify(eventos).redefinicaoSolicitada(EMAIL);
    }

    @Test
    void redefinirComTokenInexistenteDa400() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redefinir("token-do-link", "nova12345"))
                .isInstanceOf(TokenRedefinicaoInvalidoException.class)
                .hasMessage(RedefinicaoSenhaService.LINK_INVALIDO);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void redefinirComTokenVencidoDa400() {
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link")))
                .thenReturn(Optional.of(token(AGORA, null)));

        assertThatThrownBy(() -> service.redefinir("token-do-link", "nova12345"))
                .isInstanceOf(TokenRedefinicaoInvalidoException.class)
                .hasMessage(RedefinicaoSenhaService.LINK_INVALIDO);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void redefinirComTokenJaUsadoDa400() {
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link")))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(20)), AGORA.minus(Duration.ofMinutes(1)))));

        assertThatThrownBy(() -> service.redefinir("token-do-link", "nova12345"))
                .isInstanceOf(TokenRedefinicaoInvalidoException.class)
                .hasMessage(RedefinicaoSenhaService.LINK_INVALIDO);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void redefinirQuePerdeACorridaParaOutraRequisicaoDa400() {
        TokenRedefinicaoSenha registro = token(AGORA.plus(Duration.ofMinutes(20)), null);
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link"))).thenReturn(Optional.of(registro));
        when(tokenRepository.marcarUsado(registro.getId(), AGORA)).thenReturn(0);

        assertThatThrownBy(() -> service.redefinir("token-do-link", "nova12345"))
                .isInstanceOf(TokenRedefinicaoInvalidoException.class)
                .hasMessage(RedefinicaoSenhaService.LINK_INVALIDO);
        verifyNoInteractions(passwordEncoder);
        assertThat(usuario.getSenha()).isEqualTo("hash-velho");
    }

    @Test
    void redefinirTrocaASenhaComBcrypt() {
        tokenValidoNoBanco();
        when(passwordEncoder.encode("nova12345")).thenReturn("hash-novo");

        service.redefinir("token-do-link", "nova12345");

        assertThat(usuario.getSenha()).isEqualTo("hash-novo");
        verify(eventos).senhaRedefinida(EMAIL);
    }

    @Test
    void redefinirMarcaOTokenComoUsadoSoSeAindaNaoFoi() {
        TokenRedefinicaoSenha registro = tokenValidoNoBanco();

        service.redefinir("token-do-link", "nova12345");

        verify(tokenRepository).marcarUsado(registro.getId(), AGORA);
    }

    @Test
    void redefinirInvalidaOsOutrosTokens() {
        tokenValidoNoBanco();

        service.redefinir("token-do-link", "nova12345");

        verify(tokenRepository).invalidarPendentes(usuario, AGORA);
    }

    @Test
    void redefinirEncerraAsSessoesAbertas() {
        tokenValidoNoBanco();

        service.redefinir("token-do-link", "nova12345");

        assertThat(usuario.aceitaVersaoDeSessao(0)).isFalse();
        assertThat(usuario.aceitaVersaoDeSessao(1)).isTrue();
    }

    @Test
    void redefinirLimpaOLimiteDeTentativasDeLogin() {
        tokenValidoNoBanco();

        service.redefinir("token-do-link", "nova12345");

        verify(limiteTentativas).limpar(EMAIL);
    }
}
