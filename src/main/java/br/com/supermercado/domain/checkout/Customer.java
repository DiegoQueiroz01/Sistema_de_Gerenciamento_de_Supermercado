package br.com.supermercado.domain.checkout;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cliente em espera. A senha (ticket) é sequencial e formatada, por exemplo "S001".
 */
public record Customer(long id, String ticket, String name, boolean priority,
                       LocalDateTime arrivalTime) {
    public static final String DEFAULT_NAME = "Cliente";

    public Customer {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador do cliente deve ser positivo.");
        }
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("A senha do cliente é obrigatória.");
        }
        name = (name == null || name.isBlank()) ? DEFAULT_NAME : name.trim();
        Objects.requireNonNull(arrivalTime, "O horário de chegada é obrigatório.");
    }

    public Customer(long id, boolean priority, LocalDateTime arrivalTime) {
        this(id, formatTicket(id), DEFAULT_NAME, priority, arrivalTime);
    }

    /** Gera a senha no formato "S" + número com pelo menos três dígitos (S001, S042, S1000). */
    public static String formatTicket(long sequence) {
        return String.format("S%03d", sequence);
    }

    @Override
    public String toString() {
        return ticket + " " + name + (priority ? " [PRIO]" : "");
    }
}
