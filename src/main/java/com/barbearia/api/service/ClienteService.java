package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.cliente.ClienteAtualizacaoRequest;
import com.barbearia.api.dto.cliente.ClienteRapidoRequest;
import com.barbearia.api.dto.cliente.ClienteResumoResponse;
import com.barbearia.api.exceptions.ClienteNaoEncontradoException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.exceptions.VinculoNaoEncontradoException;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ClienteService {

    private static final String TELEFONE_EM_USO = "Já existe um cliente com esse telefone.";

    private final ClienteRepository clienteRepository;
    private final BarbeiroLogado barbeiroLogado;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ClienteResumoResponse cadastrarPeloBarbeiro(Usuario logado, ClienteRapidoRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        if (clienteRepository.existsByTelefoneAndBarbeiroId(data.telefone(), barbeiro.getId())) {
            throw new UsuarioExistenteException(TELEFONE_EM_USO);
        }

        Cliente cliente = Cliente.builder()
                .barbeiro(barbeiro)
                .nome(data.nome())
                .telefone(data.telefone())
                .build();
        clienteRepository.save(cliente);

        return resumo(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResumoResponse> listarMeusClientes(Usuario logado) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return clienteRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId()).stream()
                .map(this::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResumoResponse buscarMeuCliente(Usuario logado, UUID clienteId) {
        return resumo(buscarCliente(logado, clienteId));
    }

    @Transactional
    public ClienteResumoResponse atualizarCliente(Usuario logado, UUID clienteId,
                                                  ClienteAtualizacaoRequest data) {
        Cliente cliente = buscarCliente(logado, clienteId);

        if (data.telefone() != null && !data.telefone().equals(cliente.getTelefone())) {
            if (clienteRepository.existsByTelefoneAndBarbeiroId(data.telefone(), cliente.getBarbeiro().getId())) {
                throw new UsuarioExistenteException(TELEFONE_EM_USO);
            }
            cliente.setTelefone(data.telefone());
        }

        if (data.nome() != null) {
            cliente.setNome(data.nome());
        }

        return resumo(cliente);
    }

    @Transactional
    public ClienteResumoResponse aprovarVinculo(Usuario logado, UUID clienteId) {
        Cliente cliente = buscarCliente(logado, clienteId);
        Usuario pendente = buscarPendente(cliente);

        cliente.setUsuarioPendente(null);
        cliente.setUsuario(pendente);

        return resumo(cliente);
    }

    @Transactional
    public void recusarVinculo(Usuario logado, UUID clienteId) {
        Cliente cliente = buscarCliente(logado, clienteId);
        Usuario pendente = buscarPendente(cliente);

        cliente.setUsuarioPendente(null);
        clienteRepository.saveAndFlush(cliente);
        usuarioRepository.delete(pendente);
    }

    private Cliente buscarCliente(Usuario logado, UUID clienteId) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return clienteRepository.findByIdAndBarbeiroId(clienteId, barbeiro.getId())
                .orElseThrow(() -> new ClienteNaoEncontradoException("Cliente não encontrado."));
    }

    private Usuario buscarPendente(Cliente cliente) {
        if (cliente.getUsuarioPendente() == null) {
            throw new VinculoNaoEncontradoException("Esse cliente não tem pedido de vínculo.");
        }
        return cliente.getUsuarioPendente();
    }

    private ClienteResumoResponse resumo(Cliente cliente) {
        Usuario pendente = cliente.getUsuarioPendente();
        ClienteResumoResponse.PedidoVinculo pedido = pendente == null
                ? null
                : new ClienteResumoResponse.PedidoVinculo(pendente.getNome(), pendente.getEmail());

        return new ClienteResumoResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                cliente.getUsuario() != null, pedido);
    }
}
