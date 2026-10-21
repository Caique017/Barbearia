package com.barbearia.api.compartilhado.seguranca;

public record TokenAutenticacao(String email, int versaoSessao) {
}
