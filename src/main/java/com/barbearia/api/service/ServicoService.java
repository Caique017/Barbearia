package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.servico.ServicoAtualizacaoRequest;
import com.barbearia.api.dto.servico.ServicoPublicoResponse;
import com.barbearia.api.dto.servico.ServicoRequest;
import com.barbearia.api.dto.servico.ServicoResponse;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.exceptions.ServicoDuplicadoException;
import com.barbearia.api.exceptions.ServicoNaoEncontradoException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private static final String NOME_EM_USO = "Já existe um serviço com esse nome.";

    private final ServicoRepository servicoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final BarbeiroLogado barbeiroLogado;

    @Transactional
    public ServicoResponse criar(Usuario logado, ServicoRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        if (servicoRepository.existsByBarbeiroIdAndNomeIgnoreCase(barbeiro.getId(), data.nome())) {
            throw new ServicoDuplicadoException(NOME_EM_USO);
        }

        Servico servico = Servico.builder()
                .barbeiro(barbeiro)
                .nome(data.nome())
                .preco(data.preco())
                .duracaoMinutos(data.duracaoMinutos())
                .custoInsumo(data.custoInsumo())
                .build();
        servicoRepository.save(servico);

        return resposta(servico);
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(Usuario logado) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return servicoRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId()).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(Usuario logado, UUID id) {
        return resposta(buscarServico(logado, id));
    }

    @Transactional
    public ServicoResponse atualizar(Usuario logado, UUID id, ServicoAtualizacaoRequest data) {
        Servico servico = buscarServico(logado, id);

        if (data.nome() != null && !data.nome().equalsIgnoreCase(servico.getNome())) {
            if (servicoRepository.existsByBarbeiroIdAndNomeIgnoreCase(servico.getBarbeiro().getId(), data.nome())) {
                throw new ServicoDuplicadoException(NOME_EM_USO);
            }
        }

        if (data.nome() != null) {
            servico.setNome(data.nome());
        }
        if (data.preco() != null) {
            servico.setPreco(data.preco());
        }
        if (data.duracaoMinutos() != null) {
            servico.setDuracaoMinutos(data.duracaoMinutos());
        }
        if (data.custoInsumo() != null) {
            servico.setCustoInsumo(data.custoInsumo());
        }
        if (data.ativo() != null) {
            servico.setAtivo(data.ativo());
        }

        return resposta(servico);
    }

    @Transactional(readOnly = true)
    public List<ServicoPublicoResponse> listarPublicos(String slug) {
        Barbeiro barbeiro = barbeiroRepository.findBySlug(slug)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Barbearia não encontrada."));

        return servicoRepository.findByBarbeiroIdAndAtivoTrueOrderByNomeAsc(barbeiro.getId()).stream()
                .map(servico -> new ServicoPublicoResponse(servico.getId(), servico.getNome(), servico.getPreco(),
                        servico.getDuracaoMinutos()))
                .toList();
    }

    private Servico buscarServico(Usuario logado, UUID id) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return servicoRepository.findByIdAndBarbeiroId(id, barbeiro.getId())
                .orElseThrow(() -> new ServicoNaoEncontradoException("Serviço não encontrado."));
    }

    private ServicoResponse resposta(Servico servico) {
        return new ServicoResponse(servico.getId(), servico.getNome(), servico.getPreco(),
                servico.getDuracaoMinutos(), servico.getCustoInsumo(), servico.isAtivo());
    }
}
