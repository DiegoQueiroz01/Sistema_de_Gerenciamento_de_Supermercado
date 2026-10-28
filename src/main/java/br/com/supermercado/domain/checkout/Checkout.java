package br.com.supermercado.domain.checkout;

import java.util.List;

// Caixa de atendimento: tem uma fila, pode ter um cliente sendo atendido
// e pode estar aberto ou fechado.
public class Checkout {
    private final String id;
    private final AttendanceQueue queue;
    private CheckoutStatus status;
    private Customer customerInService; // null quando ninguém está sendo atendido

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

    public Customer customerInService() {
        return customerInService;
    }

    public void open() {
        if (status == CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa já está aberto: " + id);
        }
        status = CheckoutStatus.OPEN;
    }

    public void close() {
        if (!queue.isEmpty()) {
            throw new IllegalStateException("Não é possível fechar um caixa com clientes aguardando.");
        }
        if (customerInService != null) {
            throw new IllegalStateException("Conclua o atendimento em andamento antes de fechar o caixa.");
        }
        if (status == CheckoutStatus.CLOSED) {
            throw new IllegalStateException("O caixa já está fechado: " + id);
        }
        status = CheckoutStatus.CLOSED;
    }

    public void enqueue(Customer customer) {
        checkOpen();
        queue.enqueue(customer);
    }

    public void enqueueByArrival(Customer customer) {
        checkOpen();
        queue.enqueueByArrival(customer);
    }

    public Customer removeNextCustomer() {
        checkOpen();
        return queue.removeNext();
    }

    public Customer peekNextCustomer() {
        checkOpen();
        return queue.peekNext();
    }

    public Customer removeCustomer(long customerId) {
        checkOpen();
        return queue.remove(customerId);
    }

    public boolean hasCustomer(long customerId) {
        return queue.contains(customerId);
    }

    public Customer findWaitingByTicket(String ticket) {
        return queue.findByTicket(ticket);
    }

    public void startService(Customer customer) {
        checkOpen();
        if (customer == null) {
            throw new IllegalArgumentException("O cliente é obrigatório.");
        }
        if (customerInService != null) {
            throw new IllegalStateException("Já existe um cliente em atendimento no caixa " + id + ".");
        }
        customerInService = customer;
    }

    // Termina o atendimento atual e devolve o cliente que estava sendo atendido (ou null)
    public Customer finishService() {
        Customer finished = customerInService;
        customerInService = null;
        return finished;
    }

    private void checkOpen() {
        if (status != CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa não está aberto: " + id);
        }
    }
}
