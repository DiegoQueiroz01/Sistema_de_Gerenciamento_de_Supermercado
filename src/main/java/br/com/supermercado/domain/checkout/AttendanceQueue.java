package br.com.supermercado.domain.checkout;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fila de um caixa: FIFO para clientes comuns; prioritários entram depois dos
 * prioritários já presentes e antes dos comuns. Tem capacidade máxima.
 */
public final class AttendanceQueue {
    public static final int DEFAULT_CAPACITY = 100;

    private final LinkedList<Customer> customers = new LinkedList<>();
    private final int capacity;

    public AttendanceQueue() {
        this(DEFAULT_CAPACITY);
    }

    public AttendanceQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A capacidade da fila deve ser positiva.");
        }
        this.capacity = capacity;
    }

    public void enqueue(Customer customer) {
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        ensureCanAdd(customer.id());

        if (!customer.priority()) {
            customers.addLast(customer);
            return;
        }

        int insertionIndex = 0;
        while (insertionIndex < customers.size()
                && customers.get(insertionIndex).priority()) {
            insertionIndex++;
        }
        customers.add(insertionIndex, customer);
    }

    /**
     * Recebe um cliente vindo de outra fila (transferência ou fechamento de caixa):
     * prioritários continuam à frente dos comuns e, dentro de cada grupo,
     * a posição segue o horário de chegada.
     */
    public void enqueueByArrival(Customer customer) {
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        ensureCanAdd(customer.id());

        int insertionIndex = 0;
        while (insertionIndex < customers.size()
                && goesBefore(customers.get(insertionIndex), customer)) {
            insertionIndex++;
        }
        customers.add(insertionIndex, customer);
    }

    private static boolean goesBefore(Customer queued, Customer incoming) {
        if (queued.priority() != incoming.priority()) {
            return queued.priority();
        }
        return !queued.arrivalTime().isAfter(incoming.arrivalTime());
    }

    public Optional<Customer> peekNext() {
        return Optional.ofNullable(customers.peekFirst());
    }

    public Optional<Customer> removeNext() {
        return customers.isEmpty()
                ? Optional.empty()
                : Optional.of(customers.removeFirst());
    }

    public Optional<Customer> remove(long customerId) {
        for (int index = 0; index < customers.size(); index++) {
            if (customers.get(index).id() == customerId) {
                return Optional.of(customers.remove(index));
            }
        }
        return Optional.empty();
    }

    public boolean contains(long customerId) {
        return customers.stream().anyMatch(customer -> customer.id() == customerId);
    }

    public Optional<Customer> findByTicket(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        return customers.stream()
                .filter(customer -> customer.ticket().equalsIgnoreCase(ticket.trim()))
                .findFirst();
    }

    public int size() {
        return customers.size();
    }

    public int capacity() {
        return capacity;
    }

    public int freeSlots() {
        return capacity - customers.size();
    }

    public boolean isEmpty() {
        return customers.isEmpty();
    }

    public boolean isFull() {
        return customers.size() >= capacity;
    }

    public List<Customer> snapshot() {
        return List.copyOf(customers);
    }

    private void ensureCanAdd(long customerId) {
        if (contains(customerId)) {
            throw new IllegalArgumentException(
                    "O cliente já está nesta fila: " + customerId);
        }
        if (isFull()) {
            throw new IllegalStateException("A fila está cheia (capacidade " + capacity + ").");
        }
    }
}
