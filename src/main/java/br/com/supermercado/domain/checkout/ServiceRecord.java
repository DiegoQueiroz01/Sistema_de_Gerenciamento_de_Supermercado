package br.com.supermercado.domain.checkout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record ServiceRecord(
        long id,
        String checkoutId,
        long customerId,
        LocalDateTime arrivalTime,
        LocalDateTime serviceStartedAt) {

    public ServiceRecord {
        if (id <= 0 || customerId <= 0) {
            throw new IllegalArgumentException("Os identificadores devem ser positivos.");
        }
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        Objects.requireNonNull(arrivalTime, "O horário de chegada é obrigatório.");
        Objects.requireNonNull(serviceStartedAt, "O início do atendimento é obrigatório.");
        if (serviceStartedAt.isBefore(arrivalTime)) {
            throw new IllegalArgumentException(
                    "O atendimento não pode começar antes da chegada do cliente.");
        }
    }

    public Duration calculateWaitTime() {
        return Duration.between(arrivalTime, serviceStartedAt);
    }
}