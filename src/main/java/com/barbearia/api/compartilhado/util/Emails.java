package com.barbearia.api.compartilhado.util;

import java.util.Locale;

public final class Emails {

    private Emails() {
    }

    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String mascarar(String email) {
        if (email == null) {
            return null;
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }
}
