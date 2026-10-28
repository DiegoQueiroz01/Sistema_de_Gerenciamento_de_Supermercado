package br.com.supermercado.domain.checkout;

import java.time.Duration;
import java.time.LocalDateTime;

// Registro de um atendimento: qual cliente, em qual caixa e quando começou.
public class ServiceRecord {
    private final long id;
    private final String checkoutId;
    private final Customer customer;
    private final LocalDateTime serviceStartedAt;

    public ServiceRecord(long id, String checkoutId, Customer customer, LocalDateTime serviceStartedAt) {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador do registro deve ser positivo.");
        }
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        if (customer == null) {
            throw new IllegalArgumentException("O cliente é obrigatório.");
        }
        if (serviceStartedAt == null) {
            throw new IllegalArgumentException("O início do atendimento é obrigatório.");
        }
        if (serviceStartedAt.isBefore(customer.arrivalTime())) {
            throw new IllegalArgumentException("O atendimento não pode começar antes da chegada do cliente.");
        }
        this.id = id;
        this.checkoutId = checkoutId;
        this.customer = customer;
        this.serviceStartedAt = serviceStartedAt;
    }

    public long id() { return id; }
    public String checkoutId() { return checkoutId; }
    public Customer customer() { return customer; }
    public LocalDateTime serviceStartedAt() { return serviceStartedAt; }

    public long customerId() {
        return customer.id();
    }

    public LocalDateTime arrivalTime() {
        return customer.arrivalTime();
    }

    // Tempo entre a chegada do cliente e o início do atendimento
    public Duration calculateWaitTime() {
        return Duration.between(customer.arrivalTime(), serviceStartedAt);
    }
}
