package com.barbearia.api.auth;

import com.barbearia.api.barbeiro.Barbeiro;
import com.barbearia.api.barbeiro.BarbeiroCadastroRequest;
import com.barbearia.api.barbeiro.BarbeiroCadastroResponse;
import com.barbearia.api.barbeiro.BarbeiroLoginRequest;
import com.barbearia.api.barbeiro.BarbeiroLoginResponse;
import com.barbearia.api.barbeiro.BarbeiroNaoEncontradoException;
import com.barbearia.api.barbeiro.BarbeiroRepository;
import com.barbearia.api.barbeiro.GeradorSlug;
import com.barbearia.api.cliente.Cliente;
import com.barbearia.api.cliente.ClienteCadastroRequest;
import com.barbearia.api.cliente.ClienteCadastroResponse;
import com.barbearia.api.cliente.ClienteLoginRequest;
import com.barbearia.api.cliente.ClienteLoginResponse;
import com.barbearia.api.cliente.ClienteRepository;
import com.barbearia.api.compartilhado.seguranca.EventosSeguranca;
import com.barbearia.api.compartilhado.seguranca.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";
    private static final String HASH_FICTICIO = "$2a$10$e9/Xxgf3excYTfkmQBveiOCNXmdDJP9j.PW8kb5ETvtiq/IwoYYMC";

    private final UsuarioRepository usuarioRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimiteTentativasLogin limiteTentativas;
    private final EventosSeguranca eventos;
    private final GeradorSlug geradorSlug;

    @Transactional
    public BarbeiroCadastroResponse cadastroBarbeiro(BarbeiroCadastroRequest data) {
        garantirEmailLivre(data.email());

        Usuario usuario = criarUsuario(data.nome(), data.email(), data.senha(), Role.BARBEIRO);

        Barbeiro barbeiro = Barbeiro.builder()
                .usuario(usuario)
                .nomeBarbearia(data.nomeBarbearia())
                .horarioFuncionamento(data.horarioFuncionamento())
                .slug(geradorSlug.gerar(data.nomeBarbearia()))
                .build();
        barbeiroRepository.save(barbeiro);

        return new BarbeiroCadastroResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail(),
                barbeiro.getSlug());
    }

    @Transactional(readOnly = true)
    public BarbeiroLoginResponse loginBarbeiro(BarbeiroLoginRequest data) {
        Usuario usuario = login(data.email(), data.senha(), Role.BARBEIRO);

        return new BarbeiroLoginResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public Usuario login(String email, String senha, Role rolePermitida) {
        try {
            limiteTentativas.verificar(email);
        } catch (TentativasExcedidasException e) {
            eventos.loginBloqueado(email);
            throw e;
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(encontrado -> encontrado.getRole() == rolePermitida)
                .orElse(null);

        boolean senhaConfere = passwordEncoder.matches(senha, usuario != null ? usuario.getSenha() : HASH_FICTICIO);

        if (usuario == null || !senhaConfere) {
            limiteTentativas.registrarFalha(email);
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
            throw new UsuarioExistenteException("Esse telefone já possui uma conta.");
        }

        if (existente != null && existente.getUsuarioPendente() != null) {
            throw new UsuarioExistenteException(
                    "Já existe um pedido de vínculo para esse telefone aguardando o barbeiro.");
        }

        Usuario usuario = criarUsuario(data.nome(), data.email(), data.senha(), Role.CLIENTE);

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
    public ClienteLoginResponse loginCliente(ClienteLoginRequest data) {
        Usuario usuario = login(data.email(), data.senha(), Role.CLIENTE);

        return new ClienteLoginResponse(tokenService.gerarToken(usuario), usuario.getNome(), usuario.getEmail());
    }

    private void garantirEmailLivre(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new UsuarioExistenteException("O e-mail " + email + " já está em uso.");
        }
    }

    private Usuario criarUsuario(String nome, String email, String senha, Role role) {
        Usuario usuario = Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(role)
                .build();
        usuarioRepository.save(usuario);
        return usuario;
    }
}
