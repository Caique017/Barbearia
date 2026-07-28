package com.barbearia.api.util;

public final class Telefone {

    public static final String PADRAO = "[1-9]{2}9?\\d{8}";

    private Telefone() {
    }

    public static String normalizar(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }
}
