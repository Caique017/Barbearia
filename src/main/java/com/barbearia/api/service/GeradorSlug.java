package com.barbearia.api.service;

import com.barbearia.api.exceptions.SlugIndisponivelException;
import com.barbearia.api.repositories.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GeradorSlug {

    private static final int TAMANHO_MAXIMO = 110;
    private static final String PADRAO = "barbearia";
    private static final Set<String> RESERVADOS = Set.of("me", "admin", "api", "publico");

    private final BarbeiroRepository barbeiroRepository;

    public String gerar(String nomeBarbearia) {
        String base = normalizar(nomeBarbearia);
        if (base.isEmpty()) {
            base = PADRAO;
        }

        String candidato = base;
        int sufixo = 1;
        while (RESERVADOS.contains(candidato) || barbeiroRepository.existsBySlug(candidato)) {
            sufixo++;
            candidato = base + "-" + sufixo;
        }
        return candidato;
    }

    public void validarEscolhido(String slug, String slugAtual) {
        if (RESERVADOS.contains(slug)) {
            throw new SlugIndisponivelException("O link '" + slug + "' é reservado. Escolha outro.");
        }

        if (!slug.equals(slugAtual) && barbeiroRepository.existsBySlug(slug)) {
            throw new SlugIndisponivelException("O link '" + slug + "' já está em uso.");
        }
    }

    private String normalizar(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (base.length() > TAMANHO_MAXIMO) {
            base = base.substring(0, TAMANHO_MAXIMO).replaceAll("-+$", "");
        }
        return base;
    }
}
