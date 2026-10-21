package com.barbearia.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.barbearia.api.domain.Usuario;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    private static final String ISSUER = "barbearia-api";
    private static final String CLAIM_VERSAO_SESSAO = "sv";
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private long expirationHours;

    @PostConstruct
    void validarSegredo() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("Variável de ambiente API_SECURITY_TOKEN_SECRET ausente ou com menos de "
                    + TAMANHO_MINIMO_SEGREDO + " bytes. Use um valor aleatório, por exemplo 48 bytes em base64.");
        }
    }

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getEmail())
                    .withClaim("role", usuario.getRole().name())
                    .withClaim(CLAIM_VERSAO_SESSAO, usuario.getVersaoSessao())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(dataExpiracao())
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar token JWT.", e);
        }
    }

    public TokenAutenticacao validarToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaimPresence(CLAIM_VERSAO_SESSAO)
                    .build()
                    .verify(token);
            return new TokenAutenticacao(jwt.getSubject(), jwt.getClaim(CLAIM_VERSAO_SESSAO).asInt());
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    private Instant dataExpiracao() {
        return Instant.now().plus(expirationHours, ChronoUnit.HOURS);
    }
}
