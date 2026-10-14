package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.barbeiro.BarbeiroAtualizacaoRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroLoginRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroLoginResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroPerfilResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroPublicoResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroSlugRequest;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.exceptions.SlugIndisponivelException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private static final int TAMANHO_MAXIMO_SLUG = 110;
    private static final String SLUG_PADRAO = "barbearia";
    private static final Set<String> SLUGS_RESERVADOS = Set.of("me", "admin", "api", "publico");

    private final UsuarioRepository usuarioRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthService authService;

    @Transactional
    public BarbeiroCadastroResponse registroBarbeiro(BarbeiroCadastroRequest data) {
        if (usuarioRepository.findByEmail(data.email()).isPresent()) {
            throw new UsuarioExistenteException("O e-mail " + data.email() + " já está em uso.");
        }

        Usuario usuario = Usuario.builder()
                .nome(data.nome())
                .email(data.email())
                .senha(passwordEncoder.encode(data.senha()))
                .role(Role.BARBEIRO)
                .build();
        usuarioRepository.save(usuario);

        Barbeiro barbeiro = Barbeiro.builder()
                .usuario(usuario)
                .nomeBarbearia(data.nomeBarbearia())
                .horarioFuncionamento(data.horarioFuncionamento())
                .slug(gerarSlug(data.nomeBarbearia()))
                .build();
        barbeiroRepository.save(barbeiro);

        String token = tokenService.gerarToken(usuario);

        return new BarbeiroCadastroResponse(token, usuario.getNome(), usuario.getEmail(), barbeiro.getSlug());
    }

    public BarbeiroLoginResponse loginBarbeiro(BarbeiroLoginRequest data) {
        Usuario usuario = authService.login(data.email(), data.senha(), Role.BARBEIRO);

        String token = tokenService.gerarToken(usuario);
        return new BarbeiroLoginResponse(usuario.getNome(), usuario.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public BarbeiroPerfilResponse buscarPerfil(Usuario usuario) {
        return perfil(buscarBarbeiro(usuario), usuario);
    }

    @Transactional
    public BarbeiroPerfilResponse atualizarPerfil(Usuario logado, BarbeiroAtualizacaoRequest data) {
        Barbeiro barbeiro = buscarBarbeiro(logado);

        if (data.nomeBarbearia() != null) {
            barbeiro.setNomeBarbearia(data.nomeBarbearia());
        }

        if (data.horarioFuncionamento() != null) {
            barbeiro.setHorarioFuncionamento(data.horarioFuncionamento());
        }

        return perfil(barbeiro, logado);
    }

    @Transactional
    public BarbeiroPerfilResponse atualizarSlug(Usuario logado, BarbeiroSlugRequest data) {
        Barbeiro barbeiro = buscarBarbeiro(logado);
        String slug = data.slug();

        if (SLUGS_RESERVADOS.contains(slug)) {
            throw new SlugIndisponivelException("O link '" + slug + "' é reservado. Escolha outro.");
        }

        if (!slug.equals(barbeiro.getSlug()) && barbeiroRepository.existsBySlug(slug)) {
            throw new SlugIndisponivelException("O link '" + slug + "' já está em uso.");
        }

        barbeiro.setSlug(slug);

        return perfil(barbeiro, logado);
    }

    @Transactional(readOnly = true)
    public BarbeiroPublicoResponse buscarPublico(String slug) {
        Barbeiro barbeiro = barbeiroRepository.findBySlug(slug)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Barbearia não encontrada."));

        return new BarbeiroPublicoResponse(
                barbeiro.getId(),
                barbeiro.getNomeBarbearia(),
                barbeiro.getHorarioFuncionamento()
        );
    }

    private Barbeiro buscarBarbeiro(Usuario usuario) {
        return barbeiroRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Perfil de barbeiro não encontrado."));
    }

    private BarbeiroPerfilResponse perfil(Barbeiro barbeiro, Usuario usuario) {
        return new BarbeiroPerfilResponse(
                barbeiro.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                barbeiro.getNomeBarbearia(),
                barbeiro.getSlug(),
                barbeiro.getHorarioFuncionamento()
        );
    }

    private String gerarSlug(String nome) {
        String base = normalizarSlug(nome);
        if (base.isEmpty()) {
            base = SLUG_PADRAO;
        }

        String candidato = base;
        int sufixo = 1;
        while (SLUGS_RESERVADOS.contains(candidato) || barbeiroRepository.existsBySlug(candidato)) {
            sufixo++;
            candidato = base + "-" + sufixo;
        }
        return candidato;
    }

    private String normalizarSlug(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (base.length() > TAMANHO_MAXIMO_SLUG) {
            base = base.substring(0, TAMANHO_MAXIMO_SLUG).replaceAll("-+$", "");
        }
        return base;
    }
}
