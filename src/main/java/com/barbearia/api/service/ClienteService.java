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

        String telefone = normalizarTelefone(data.telefone());

        if (clienteRepository.existsByTelefoneAndBarbeiroId(telefone, barbeiro.getId())) {
            throw new UsuarioExistenteException("Já existe um cliente com esse telefone.");
        }

        Cliente cliente = Cliente.builder()
                .barbeiro(barbeiro)
                .nome(data.nome())
                .telefone(telefone)
                .build();
        clienteRepository.save(cliente);

        return new ClienteResumoResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(), false);
    }

    @Transactional
    public ClienteCadastroResponse cadastroCliente(ClienteCadastroRequest data) {
        if (usuarioRepository.findByEmail(data.email()).isPresent()) {
            throw new UsuarioExistenteException("O e-mail " + data.email() + " já está em uso.");
        }

        Barbeiro barbeiro = barbeiroRepository.findById(data.barbeiroId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Barbeiro não encontrado."));

        Usuario usuario = Usuario.builder()
                .nome(data.nome())
                .email(data.email())
                .senha(passwordEncoder.encode(data.senha()))
                .role(Role.CLIENTE)
                .build();
        usuarioRepository.save(usuario);

        String telefone = normalizarTelefone(data.telefone());

        Cliente cliente = clienteRepository
                .findByTelefoneAndBarbeiroId(telefone, barbeiro.getId())
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
                    .telefone(telefone)
                    .build();
        }
        clienteRepository.save(cliente);

        String token = tokenService.gerarToken(usuario);
        return new ClienteCadastroResponse(token, usuario.getNome(), usuario.getEmail());
    }

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

    private static String normalizarTelefone(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }

    private Barbeiro buscarBarbeiro(Usuario usuario) {
        return barbeiroRepository.findByUsuario(usuario)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Perfil de barbeiro não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<ClienteResumoResponse> listarMeusClientes(Usuario barbeiroLogado) {

        Barbeiro barbeiro = buscarBarbeiro(barbeiroLogado);

        List<Cliente> clientes = clienteRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId());

        return clientes.stream().map(c -> new ClienteResumoResponse(c.getId(), c.getNome(), c.getTelefone(), c.getUsuario() != null)).toList();
    }
}