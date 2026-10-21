package com.barbearia.api.barbeiro;

import com.barbearia.api.auth.Usuario;
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
