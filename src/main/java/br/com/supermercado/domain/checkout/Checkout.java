package br.com.supermercado.domain.checkout;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caixa de atendimento: fila própria, cliente em atendimento e estado aberto/fechado.
 * Para fechar um caixa com clientes aguardando use QueueService.closeCheckout,
 * que redistribui os clientes antes.
 */
public final class Checkout {
    private final String id;
    private final AttendanceQueue queue;
    private CheckoutStatus status;
    private Customer customerInService;

    public Checkout(String id) {
        this(id, AttendanceQueue.DEFAULT_CAPACITY);
    }

    public Checkout(String id, int queueCapacity) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        this.id = id;
        this.queue = new AttendanceQueue(queueCapacity);
        this.status = CheckoutStatus.CLOSED;
    }

    public String id() {
        return id;
    }

    public CheckoutStatus status() {
        return status;
    }

    public boolean isOpen() {
        return status == CheckoutStatus.OPEN;
    }

    public int waitingCount() {
        return queue.size();
    }

    public int queueCapacity() {
        return queue.capacity();
    }

    public int freeSlots() {
        return queue.freeSlots();
    }

    public boolean isQueueFull() {
        return queue.isFull();
    }

    public List<Customer> waitingCustomers() {
        return queue.snapshot();
    }

    public Optional<Customer> customerInService() {
        return Optional.ofNullable(customerInService);
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
        if (customerInService != null) {
            throw new IllegalStateException(
                    "Conclua o atendimento em andamento antes de fechar o caixa.");
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

    /** Recebe cliente transferido: prioridade primeiro, depois ordem de chegada. */
    public void enqueueByArrival(Customer customer) {
        requireOpen();
        queue.enqueueByArrival(Objects.requireNonNull(customer, "O cliente é obrigatório."));
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

    public Optional<Customer> findWaitingByTicket(String ticket) {
        return queue.findByTicket(ticket);
    }

    /** Coloca o cliente como "em atendimento" neste caixa. */
    public void startService(Customer customer) {
        requireOpen();
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        if (customerInService != null) {
            throw new IllegalStateException(
                    "Já existe um cliente em atendimento no caixa " + id + ".");
        }
        customerInService = customer;
    }

    /** Conclui o atendimento em andamento; devolve o cliente atendido, se houver. */
    public Optional<Customer> finishService() {
        Customer finished = customerInService;
        customerInService = null;
        return Optional.ofNullable(finished);
    }

    private void requireOpen() {
        if (status != CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa não está aberto: " + id);
        }
    }
}
