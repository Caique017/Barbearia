package com.barbearia.api.security;

public record TokenAutenticacao(String email, int versaoSessao) {
}
