package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroCadastroResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroLoginRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroLoginResponse;
import com.barbearia.api.dto.cliente.ClienteCadastroRequest;
import com.barbearia.api.dto.cliente.ClienteCadastroResponse;
import com.barbearia.api.dto.cliente.ClienteLoginRequest;
import com.barbearia.api.dto.cliente.ClienteLoginResponse;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.exceptions.CredenciaisInvalidasException;
import com.barbearia.api.exceptions.TentativasExcedidasException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.EventosSeguranca;
import com.barbearia.api.security.LimiteTentativasLogin;
import com.barbearia.api.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";
    static final String CADASTRO_RECUSADO =
            "Não foi possível concluir o cadastro com esses dados. Se você já tem conta, entre ou recupere a senha.";
    private static final String HASH_FICTICIO = "$2a$10$e9/Xxgf3excYTfkmQBveiOCNXmdDJP9j.PW8kb5ETvtiq/IwoYYMC";

    private final UsuarioRepository usuarioRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimiteTentativasLogin limiteTentativas;
    private final EventosSeguranca eventos;
    private final GeradorSlug geradorSlug;
    private final AssinaturaService assinaturaService;

    @Transactional
    public BarbeiroCadastroResponse cadastroBarbeiro(BarbeiroCadastroRequest data) {
        garantirEmailLivre(data.email());

        Usuario usuario = criarUsuario(data.nome(), data.email(), data.senha(), Role.BARBEIRO,
                Boolean.TRUE.equals(data.aceiteMarketing()));

        Barbeiro barbeiro = Barbeiro.builder()
                .usuario(usuario)
                .nomeBarbearia(data.nomeBarbearia())
                .horarioFuncionamento(data.horarioFuncionamento())
                .slug(geradorSlug.gerar(data.nomeBarbearia()))
                .build();
        barbeiroRepository.save(barbeiro);
        assinaturaService.iniciarTeste(barbeiro);

        return new BarbeiroCadastroResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail(),
                barbeiro.getSlug());
    }

    @Transactional(readOnly = true)
    public BarbeiroLoginResponse loginBarbeiro(BarbeiroLoginRequest data, String ip) {
        Usuario usuario = login(data.email(), data.senha(), Role.BARBEIRO, ip);

        return new BarbeiroLoginResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public Usuario login(String email, String senha, Role rolePermitida, String ip) {
        try {
            limiteTentativas.verificar(email, ip);
        } catch (TentativasExcedidasException e) {
            eventos.loginBloqueado(email);
            throw e;
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(encontrado -> encontrado.getRole() == rolePermitida)
                .orElse(null);

        boolean senhaConfere = passwordEncoder.matches(senha, usuario != null ? usuario.getSenha() : HASH_FICTICIO);

        if (usuario == null || !senhaConfere) {
            limiteTentativas.registrarFalha(email, ip);
            eventos.loginFalhou(email);
            throw new CredenciaisInvalidasException(CREDENCIAIS_INVALIDAS);
        }

        limiteTentativas.limpar(email);
        return usuario;
    }

    @Transactional
    public void encerrarSessoes(Usuario logado) {
        Usuario usuario = usuarioRepository.findById(logado.getId()).orElseThrow();
        usuario.encerrarSessoes();
        eventos.sessoesEncerradas(usuario.getEmail());
    }

    @Transactional
    public ClienteCadastroResponse cadastroCliente(ClienteCadastroRequest data) {
        garantirEmailLivre(data.email());

        Barbeiro barbeiro = barbeiroRepository.findById(data.barbeiroId())
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Barbeiro não encontrado."));

        Cliente existente = clienteRepository
                .findByTelefoneAndBarbeiroId(data.telefone(), barbeiro.getId())
                .orElse(null);

        if (existente != null && existente.getUsuario() != null) {
            throw new UsuarioExistenteException(CADASTRO_RECUSADO);
        }

        if (existente != null && existente.getUsuarioPendente() != null) {
            throw new UsuarioExistenteException(CADASTRO_RECUSADO);
        }

        Usuario usuario = criarUsuario(data.nome(), data.email(), data.senha(), Role.CLIENTE,
                Boolean.TRUE.equals(data.aceiteMarketing()));

        boolean vinculoPendente = existente != null;
        if (vinculoPendente) {
            existente.setUsuarioPendente(usuario);
            clienteRepository.save(existente);
        } else {
            clienteRepository.save(Cliente.builder()
                    .usuario(usuario)
                    .barbeiro(barbeiro)
                    .nome(data.nome())
                    .telefone(data.telefone())
                    .build());
        }

        String token = tokenService.gerarToken(usuario);
        return new ClienteCadastroResponse(token, usuario.getNome(), usuario.getEmail(), vinculoPendente);
    }

    @Transactional(readOnly = true)
    public ClienteLoginResponse loginCliente(ClienteLoginRequest data, String ip) {
        Usuario usuario = login(data.email(), data.senha(), Role.CLIENTE, ip);

        return new ClienteLoginResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail());
    }

    private void garantirEmailLivre(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new UsuarioExistenteException(CADASTRO_RECUSADO);
        }
    }

    private Usuario criarUsuario(String nome, String email, String senha, Role role, boolean aceitaMarketing) {
        Usuario usuario = Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(role)
                .build();
        Instant agora = Instant.now();
        usuario.aceitarTermos(TermosDeUso.VERSAO, agora);
        usuario.definirMarketing(aceitaMarketing, agora);
        usuarioRepository.save(usuario);
        return usuario;
    }
}
