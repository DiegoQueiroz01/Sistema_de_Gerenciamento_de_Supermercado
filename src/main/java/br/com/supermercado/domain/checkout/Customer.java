package br.com.supermercado.domain.checkout;

import java.time.LocalDateTime;
import java.util.Objects;

public record Customer(long id, boolean priority, LocalDateTime arrivalTime) {
    public Customer {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador do cliente deve ser positivo.");
        }
        Objects.requireNonNull(arrivalTime, "O horário de chegada é obrigatório.");
    }
}