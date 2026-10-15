package br.com.supermercado.domain.checkout;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AttendanceQueue {
    private final LinkedList<Customer> customers = new LinkedList<>();

    public void enqueue(Customer customer) {
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        ensureCustomerNotPresent(customer.id());

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

    public void enqueueAtEnd(Customer customer) {
        Objects.requireNonNull(customer, "O cliente é obrigatório.");
        ensureCustomerNotPresent(customer.id());
        customers.addLast(customer);
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

    public int size() {
        return customers.size();
    }

    public boolean isEmpty() {
        return customers.isEmpty();
    }

    public List<Customer> snapshot() {
        return List.copyOf(customers);
    }

    private void ensureCustomerNotPresent(long customerId) {
        if (contains(customerId)) {
            throw new IllegalArgumentException(
                    "O cliente já está nesta fila: " + customerId);
        }
    }
}