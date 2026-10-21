package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
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
        if (usuarioRepository.findByEmail(data.email()).isPresent()) {
            throw new UsuarioExistenteException("O e-mail " + data.email() + " já está em uso.");
        }

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

        Usuario usuario = Usuario.builder()
                .nome(data.nome())
                .email(data.email())
                .senha(passwordEncoder.encode(data.senha()))
                .role(Role.CLIENTE)
                .build();
        usuarioRepository.save(usuario);

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

        String token = tokenService.gerarToken(usuario);
        return new ClienteLoginResponse(usuario.getNome(), usuario.getEmail(), token);
    }
}
