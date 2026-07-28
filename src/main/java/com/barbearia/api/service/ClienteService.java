package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.auth.ClienteCadastroRequest;
import com.barbearia.api.dto.auth.ClienteCadastroResponse;
import com.barbearia.api.dto.auth.ClienteLoginRequest;
import com.barbearia.api.dto.auth.ClienteLoginResponse;
import com.barbearia.api.dto.cliente.ClienteRapidoRequest;
import com.barbearia.api.dto.cliente.ClienteResumoResponse;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.exceptions.ClienteNaoEncontradoException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.exceptions.UsuarioNaoEncontradoException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClienteResumoResponse cadastrarPeloBarbeiro(Usuario barbeiroLogado, ClienteRapidoRequest data) {
        Barbeiro barbeiro = buscarBarbeiro(barbeiroLogado);

        if (clienteRepository.existsByTelefoneAndBarbeiroId(data.telefone(), barbeiro.getId())) {
            throw new UsuarioExistenteException("Já existe um cliente com esse telefone.");
        }

        Cliente cliente = Cliente.builder()
                .barbeiro(barbeiro)
                .nome(data.nome())
                .telefone(data.telefone())
                .build();
        clienteRepository.save(cliente);

        return resumo(cliente);
    }

    @Transactional
    public ClienteCadastroResponse cadastroCliente(ClienteCadastroRequest data) {
        if (usuarioRepository.findByEmail(data.email()).isPresent()) {
            throw new UsuarioExistenteException("O e-mail " + data.email() + " já está em uso.");
        }

        Barbeiro barbeiro = barbeiroRepository.findById(data.barbeiroId())
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Barbeiro não encontrado."));

        Usuario usuario = Usuario.builder()
                .nome(data.nome())
                .email(data.email())
                .senha(passwordEncoder.encode(data.senha()))
                .role(Role.CLIENTE)
                .build();
        usuarioRepository.save(usuario);

        Cliente cliente = clienteRepository
                .findByTelefoneAndBarbeiroId(data.telefone(), barbeiro.getId())
                .orElse(null);

        if (cliente != null) {
            if (cliente.getUsuario() != null) {
                throw new UsuarioExistenteException("Esse telefone já possui uma conta.");
            }
            cliente.setUsuario(usuario);
            cliente.setNome(data.nome());
        } else {
            cliente = Cliente.builder()
                    .usuario(usuario)
                    .barbeiro(barbeiro)
                    .nome(data.nome())
                    .telefone(data.telefone())
                    .build();
        }
        clienteRepository.save(cliente);

        String token = tokenService.gerarToken(usuario);
        return new ClienteCadastroResponse(token, usuario.getNome(), usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public ClienteLoginResponse loginCliente(ClienteLoginRequest data) {
        Usuario usuario = usuarioRepository.findByEmail(data.email())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("E-mail ou senha incorretos."));

        if (usuario.getRole() != Role.CLIENTE) {
            throw new UsuarioNaoEncontradoException("E-mail ou senha incorretos.");
        }

        if (!passwordEncoder.matches(data.senha(), usuario.getSenha())) {
            throw new UsuarioNaoEncontradoException("E-mail ou senha incorretos.");
        }

        String token = tokenService.gerarToken(usuario);
        return new ClienteLoginResponse(usuario.getNome(), usuario.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public List<ClienteResumoResponse> listarMeusClientes(Usuario barbeiroLogado) {
        Barbeiro barbeiro = buscarBarbeiro(barbeiroLogado);

        return clienteRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId()).stream()
                .map(this::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResumoResponse buscarMeuCliente(Usuario barbeiroLogado, UUID clienteId) {
        Barbeiro barbeiro = buscarBarbeiro(barbeiroLogado);

        Cliente cliente = clienteRepository.findByIdAndBarbeiroId(clienteId, barbeiro.getId())
                .orElseThrow(() -> new ClienteNaoEncontradoException("Cliente não encontrado."));

        return resumo(cliente);
    }

    private Barbeiro buscarBarbeiro(Usuario usuario) {
        return barbeiroRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Perfil de barbeiro não encontrado."));
    }

    private ClienteResumoResponse resumo(Cliente cliente) {
        return new ClienteResumoResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                cliente.getUsuario() != null);
    }
}
