package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.barbeiro.BarbeiroAtualizacaoRequest;
import com.barbearia.api.dto.barbeiro.BarbeiroPerfilResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroPublicoResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroSlugRequest;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.repositories.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final BarbeiroLogado barbeiroLogado;
    private final GeradorSlug geradorSlug;

    @Transactional(readOnly = true)
    public BarbeiroPerfilResponse buscarPerfil(Usuario usuario) {
        return perfil(barbeiroLogado.de(usuario), usuario);
    }

    @Transactional
    public BarbeiroPerfilResponse atualizarPerfil(Usuario logado, BarbeiroAtualizacaoRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

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
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        geradorSlug.validarEscolhido(data.slug(), barbeiro.getSlug());
        barbeiro.setSlug(data.slug());

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
}
