package com.barbearia.api.service;

import com.barbearia.api.domain.TokenRedefinicaoSenha;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.exceptions.TokenRedefinicaoInvalidoException;
import com.barbearia.api.repositories.TokenRedefinicaoSenhaRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.EventosSeguranca;
import com.barbearia.api.security.LimiteTentativasLogin;
import com.barbearia.api.security.TokenAleatorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RedefinicaoSenhaService {

    static final String LINK_INVALIDO = "Link inválido ou expirado. Peça um novo.";
    static final Duration INTERVALO_ENTRE_PEDIDOS = Duration.ofMinutes(2);

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final LimiteTentativasLogin limiteTentativas;
    private final EventosSeguranca eventos;
    private final Clock clock;

    @Value("${api.front.url}")
    private String frontUrl;

    @Value("${api.senha.token-expiracao-minutos:30}")
    private long expiracaoMinutos;

    public RedefinicaoSenhaService(UsuarioRepository usuarioRepository,
                                   TokenRedefinicaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   ApplicationEventPublisher eventPublisher,
                                   LimiteTentativasLogin limiteTentativas,
                                   EventosSeguranca eventos,
                                   Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.limiteTentativas = limiteTentativas;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public void solicitar(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return;
        }

        Instant agora = clock.instant();
        if (tokenRepository.existsByUsuarioAndCriadoEmAfter(usuario, agora.minus(INTERVALO_ENTRE_PEDIDOS))) {
            return;
        }

        tokenRepository.invalidarPendentes(usuario, agora);

        String token = TokenAleatorio.gerar();
        tokenRepository.save(TokenRedefinicaoSenha.builder()
                .usuario(usuario)
                .tokenHash(TokenAleatorio.hash(token))
                .expiraEm(agora.plus(Duration.ofMinutes(expiracaoMinutos)))
                .criadoEm(agora)
                .build());

        eventos.redefinicaoSolicitada(usuario.getEmail());
        eventPublisher.publishEvent(new RedefinicaoSenhaSolicitada(usuario.getEmail(), usuario.getNome(),
                frontUrl + "/redefinir-senha?token=" + token));
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        Instant agora = clock.instant();
        TokenRedefinicaoSenha registro = tokenRepository.findByTokenHash(TokenAleatorio.hash(token))
                .filter(encontrado -> encontrado.valido(agora))
                .orElseThrow(() -> new TokenRedefinicaoInvalidoException(LINK_INVALIDO));

        if (tokenRepository.marcarUsado(registro.getId(), agora) == 0) {
            throw new TokenRedefinicaoInvalidoException(LINK_INVALIDO);
        }

        Usuario usuario = registro.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.encerrarSessoes();
        tokenRepository.invalidarPendentes(usuario, agora);
        limiteTentativas.limpar(usuario.getEmail());
        eventos.senhaRedefinida(usuario.getEmail());
    }
}
