package br.com.supermercado.domain.checkout;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Checkout {
    private final String id;
    private final AttendanceQueue queue;
    private CheckoutStatus status;

    public Checkout(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        this.id = id;
        this.queue = new AttendanceQueue();
        this.status = CheckoutStatus.CLOSED;
    }

    public String id() {
        return id;
    }

    public CheckoutStatus status() {
        return status;
    }

    public int waitingCount() {
        return queue.size();
    }

    public List<Customer> waitingCustomers() {
        return queue.snapshot();
    }

    public void open() {
        if (status == CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa já está aberto: " + id);
        }
        status = CheckoutStatus.OPEN;
    }

    public void close() {
        if (!queue.isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível fechar um caixa com clientes aguardando.");
        }
        if (status == CheckoutStatus.CLOSED) {
            throw new IllegalStateException("O caixa já está fechado: " + id);
        }
        status = CheckoutStatus.CLOSED;
    }

    public void enqueue(Customer customer) {
        requireOpen();
        queue.enqueue(Objects.requireNonNull(customer, "O cliente é obrigatório."));
    }

    public void enqueueAtEnd(Customer customer) {
        requireOpen();
        queue.enqueueAtEnd(Objects.requireNonNull(customer, "O cliente é obrigatório."));
    }

    public Optional<Customer> removeNextCustomer() {
        requireOpen();
        return queue.removeNext();
    }

    public Optional<Customer> peekNextCustomer() {
        requireOpen();
        return queue.peekNext();
    }

    public Optional<Customer> removeCustomer(long customerId) {
        requireOpen();
        return queue.remove(customerId);
    }

    public boolean hasCustomer(long customerId) {
        return queue.contains(customerId);
    }

    private void requireOpen() {
        if (status != CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa não está aberto: " + id);
        }
    }
}