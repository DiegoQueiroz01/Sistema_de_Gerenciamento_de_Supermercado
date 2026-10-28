package br.com.supermercado.domain.checkout;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// Fila de um caixa feita com lista encadeada.
// Clientes comuns entram no final (FIFO). Prioritários entram depois dos outros
// prioritários, mas antes de todos os comuns.
public class AttendanceQueue {
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
        checkCanAdd(customer);

        if (!customer.priority()) {
            customers.addLast(customer);
            return;
        }

        // procura a posição logo depois do último prioritário
        int index = 0;
        while (index < customers.size() && customers.get(index).priority()) {
            index++;
        }
        customers.add(index, customer);
    }

    // Usado quando o cliente vem de outra fila (transferência ou caixa fechado).
    // Prioritários continuam na frente e, dentro de cada grupo, vale a hora de chegada.
    public void enqueueByArrival(Customer customer) {
        checkCanAdd(customer);

        int index = 0;
        while (index < customers.size() && goesBefore(customers.get(index), customer)) {
            index++;
        }
        customers.add(index, customer);
    }

    private boolean goesBefore(Customer queued, Customer incoming) {
        if (queued.priority() != incoming.priority()) {
            return queued.priority();
        }
        return !queued.arrivalTime().isAfter(incoming.arrivalTime());
    }

    // Devolve o primeiro da fila sem remover (null se a fila estiver vazia)
    public Customer peekNext() {
        return customers.peekFirst();
    }

    // Remove e devolve o primeiro da fila (null se a fila estiver vazia)
    public Customer removeNext() {
        return customers.pollFirst();
    }

    // Remove o cliente com esse id; devolve null se ele não estiver na fila
    public Customer remove(long customerId) {
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).id() == customerId) {
                return customers.remove(i);
            }
        }
        return null;
    }

    public boolean contains(long customerId) {
        for (Customer c : customers) {
            if (c.id() == customerId) {
                return true;
            }
        }
        return false;
    }

    public Customer findByTicket(String ticket) {
        if (ticket == null) {
            return null;
        }
        for (Customer c : customers) {
            if (c.ticket().equalsIgnoreCase(ticket.trim())) {
                return c;
            }
        }
        return null;
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

    // Cópia da fila, para mostrar na tela sem permitir alterar a original
    public List<Customer> snapshot() {
        return new ArrayList<>(customers);
    }

    private void checkCanAdd(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("O cliente é obrigatório.");
        }
        if (contains(customer.id())) {
            throw new IllegalArgumentException("O cliente já está nesta fila: " + customer.id());
        }
        if (isFull()) {
            throw new IllegalStateException("A fila está cheia (capacidade " + capacity + ").");
        }
    }
}
