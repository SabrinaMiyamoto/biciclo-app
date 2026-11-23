package com.biciclo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * E-mails que viram ADMIN ao entrar (app.admin.emails / ADMIN_EMAILS).
 * Resolve o "primeiro administrador": o autocadastro não permite ADMIN.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        List<String> emails
) {

    public AdminProperties {
        emails = emails == null
                ? List.of()
                : emails.stream()
                        .map(email -> email.trim().toLowerCase(Locale.ROOT))
                        .filter(email -> !email.isEmpty())
                        .toList();
    }

    public boolean isAdmin(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
