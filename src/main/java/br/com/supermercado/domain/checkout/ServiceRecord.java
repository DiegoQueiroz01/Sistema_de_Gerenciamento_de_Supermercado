package br.com.supermercado.domain.checkout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro histórico de um atendimento: dados do cliente, do caixa e do início do atendimento. */
public record ServiceRecord(
        long id,
        String checkoutId,
        Customer customer,
        LocalDateTime serviceStartedAt) {

    public ServiceRecord {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador do registro deve ser positivo.");
        }
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        Objects.requireNonNull(serviceStartedAt, "O início do atendimento é obrigatório.");
        if (serviceStartedAt.isBefore(customer.arrivalTime())) {
            throw new IllegalArgumentException(
                    "O atendimento não pode começar antes da chegada do cliente.");
        }
    }

    public long customerId() {
        return customer.id();
    }

    public LocalDateTime arrivalTime() {
        return customer.arrivalTime();
    }

    public Duration calculateWaitTime() {
        return Duration.between(customer.arrivalTime(), serviceStartedAt);
    }
}
