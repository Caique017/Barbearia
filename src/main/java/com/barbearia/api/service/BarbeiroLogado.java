package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.repositories.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BarbeiroLogado {

    private final BarbeiroRepository barbeiroRepository;

    public Barbeiro de(Usuario usuario) {
        return barbeiroRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Perfil de barbeiro não encontrado."));
    }
}
