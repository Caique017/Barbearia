package com.barbearia.api.service;

import com.barbearia.api.domain.TokenRedefinicaoSenha;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.TokenRedefinicaoInvalidoException;
import com.barbearia.api.repositories.TokenRedefinicaoSenhaRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.LimiteTentativasLogin;
import com.barbearia.api.security.TokenAleatorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedefinicaoSenhaServiceTest {

    private static final String EMAIL = "pessoa@ex.com";
    private static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");
    private static final String FRONT = "http://front.test";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenRedefinicaoSenhaRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private LimiteTentativasLogin limiteTentativas;

    private RedefinicaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void criarService() {
        service = new RedefinicaoSenhaService(usuarioRepository, tokenRepository, passwordEncoder, emailService,
                limiteTentativas, Clock.fixed(AGORA, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "frontUrl", FRONT);
        ReflectionTestUtils.setField(service, "expiracaoMinutos", 30L);
        usuario = Usuario.builder().nome("Pessoa").email(EMAIL).senha("hash-velho").role(Role.CLIENTE).build();
    }

    private TokenRedefinicaoSenha token(Instant expiraEm, Instant usadoEm) {
        return TokenRedefinicaoSenha.builder()
                .usuario(usuario)
                .tokenHash(TokenAleatorio.hash("token-do-link"))
                .expiraEm(expiraEm)
                .usadoEm(usadoEm)
                .criadoEm(AGORA.minus(Duration.ofMinutes(10)))
                .build();
    }

    @Test
    void solicitarComEmailSemContaNaoFazNada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.solicitar(EMAIL);

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void solicitarDeNovoEmMenosDe2MinutosNaoFazNada() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.existsByUsuarioAndCriadoEmAfter(usuario, AGORA.minus(Duration.ofMinutes(2))))
                .thenReturn(true);

        service.solicitar(EMAIL);

        verify(tokenRepository, never()).save(any());
        verify(tokenRepository, never()).invalidarPendentes(any(), any());
        verifyNoInteractions(emailService);
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
    void bancoGuardaOHashEOEmailLevaOTokenPuro() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        ArgumentCaptor<TokenRedefinicaoSenha> salvo = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);

        service.solicitar(EMAIL);

        verify(tokenRepository).save(salvo.capture());
        verify(emailService).enviarRedefinicaoSenha(eq(EMAIL), eq("Pessoa"), link.capture());

        assertThat(link.getValue()).startsWith(FRONT + "/redefinir-senha?token=");
        String tokenDoLink = link.getValue().substring((FRONT + "/redefinir-senha?token=").length());
        assertThat(salvo.getValue().getTokenHash()).isNotEqualTo(tokenDoLink);
        assertThat(salvo.getValue().getTokenHash()).isEqualTo(TokenAleatorio.hash(tokenDoLink));
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
    void redefinirTrocaASenhaComBcrypt() {
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link")))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(20)), null)));
        when(passwordEncoder.encode("nova12345")).thenReturn("hash-novo");

        service.redefinir("token-do-link", "nova12345");

        assertThat(usuario.getSenha()).isEqualTo("hash-novo");
    }

    @Test
    void redefinirMarcaOTokenComoUsado() {
        TokenRedefinicaoSenha registro = token(AGORA.plus(Duration.ofMinutes(20)), null);
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link"))).thenReturn(Optional.of(registro));

        service.redefinir("token-do-link", "nova12345");

        assertThat(registro.getUsadoEm()).isEqualTo(AGORA);
    }

    @Test
    void redefinirInvalidaOsOutrosTokens() {
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link")))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(20)), null)));

        service.redefinir("token-do-link", "nova12345");

        verify(tokenRepository).invalidarPendentes(usuario, AGORA);
    }

    @Test
    void redefinirLimpaOLimiteDeTentativasDeLogin() {
        when(tokenRepository.findByTokenHash(TokenAleatorio.hash("token-do-link")))
                .thenReturn(Optional.of(token(AGORA.plus(Duration.ofMinutes(20)), null)));

        service.redefinir("token-do-link", "nova12345");

        verify(limiteTentativas).limpar(EMAIL);
    }
}
