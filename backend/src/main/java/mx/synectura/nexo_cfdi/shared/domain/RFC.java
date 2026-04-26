package mx.synectura.nexo_cfdi.shared.domain;

import java.util.regex.Pattern;

public record RFC(String value) {
    private static final Pattern RFC_PATTERN = Pattern.compile("^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-0]{3}$");

    public RFC {
        if (value == null || !RFC_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("RFC inválido");
        }
    }
}
