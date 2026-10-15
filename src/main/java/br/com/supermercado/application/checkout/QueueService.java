package br.com.supermercado.application.checkout;

import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.CheckoutStatus;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class QueueService {
    private final List<Checkout> checkouts = new ArrayList<>();
    private final List<ServiceRecord> serviceRecords = new ArrayList<>();
    private long nextCustomerId = 1;
    private long nextServiceRecordId = 1;

    public Checkout openCheckout(String checkoutId) {
        Checkout checkout = findCheckout(checkoutId)
                .orElseGet(() -> {
                    Checkout newCheckout = new Checkout(checkoutId);
                    checkouts.add(newCheckout);
                    return newCheckout;
                });
        checkout.open();
        return checkout;
    }

    public void closeCheckout(String checkoutId) {
        Checkout checkout = requireCheckout(checkoutId);
        if (checkout.status() != CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa já está fechado: " + checkoutId);
        }

        if (checkout.waitingCount() > 0
                && checkouts.stream().noneMatch(other ->
                other != checkout && other.status() == CheckoutStatus.OPEN)) {
            throw new IllegalStateException(
                    "Abra outro caixa antes de fechar este: há clientes aguardando.");
        }

        while (checkout.waitingCount() > 0) {
            Customer customer = checkout.removeNextCustomer().orElseThrow();
            Checkout destination = findShortestOpenCheckout(checkout)
                    .orElseThrow(() -> new IllegalStateException(
                            "Não há outro caixa aberto para receber os clientes."));
            destination.enqueueAtEnd(customer);
        }
        checkout.close();
    }

    public CustomerAssignment registerArrival(boolean priority, LocalDateTime arrivalTime) {
        Objects.requireNonNull(arrivalTime, "O horário de chegada é obrigatório.");
        Checkout checkout = findShortestOpenCheckout(null)
                .orElseThrow(() -> new IllegalStateException(
                        "Não há caixas abertos para receber clientes."));

        Customer customer = new Customer(nextCustomerId++, priority, arrivalTime);
        checkout.enqueue(customer);
        return new CustomerAssignment(customer, checkout.id());
    }

    public void transferCustomer(long customerId, String sourceCheckoutId,
                                 String destinationCheckoutId) {
        Checkout source = requireCheckout(sourceCheckoutId);
        Checkout destination = requireCheckout(destinationCheckoutId);
        if (sourceCheckoutId.equals(destinationCheckoutId)) {
            throw new IllegalArgumentException("A origem e o destino devem ser diferentes.");
        }

        if (source.status() != CheckoutStatus.OPEN
                || destination.status() != CheckoutStatus.OPEN) {
            throw new IllegalStateException("Origem e destino devem estar abertos.");
        }
        if (destination.hasCustomer(customerId)) {
            throw new IllegalArgumentException(
                    "O cliente já está na fila de destino: " + customerId);
        }

        Customer customer = source.removeCustomer(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O cliente não está na fila de origem: " + customerId));
        destination.enqueueAtEnd(customer);
    }

    public Optional<ServiceRecord> startNextAttendance(
            String checkoutId, LocalDateTime serviceStartedAt) {
        Objects.requireNonNull(serviceStartedAt, "O horário de atendimento é obrigatório.");
        Checkout checkout = requireCheckout(checkoutId);
        Customer customer = checkout.peekNextCustomer().orElse(null);
        if (customer == null) {
            return Optional.empty();
        }

        ServiceRecord record = new ServiceRecord(
                nextServiceRecordId++,
                checkout.id(),
                customer.id(),
                customer.arrivalTime(),
                serviceStartedAt);
        checkout.removeNextCustomer().orElseThrow();
        serviceRecords.add(record);
        return Optional.of(record);
    }

    public List<Checkout> checkouts() {
        return List.copyOf(checkouts);
    }

    public List<ServiceRecord> serviceRecords() {
        return List.copyOf(serviceRecords);
    }

    private Optional<Checkout> findShortestOpenCheckout(Checkout excludedCheckout) {
        return checkouts.stream()
                .filter(checkout -> checkout != excludedCheckout)
                .filter(checkout -> checkout.status() == CheckoutStatus.OPEN)
                .min(Comparator.comparingInt(Checkout::waitingCount));
    }

    private Checkout requireCheckout(String checkoutId) {
        return findCheckout(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Caixa não encontrado: " + checkoutId));
    }

    private Optional<Checkout> findCheckout(String checkoutId) {
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        return checkouts.stream()
                .filter(checkout -> checkout.id().equals(checkoutId))
                .findFirst();
    }

    public record CustomerAssignment(Customer customer, String checkoutId) {
        public CustomerAssignment {
            Objects.requireNonNull(customer, "O cliente é obrigatório.");
            if (checkoutId == null || checkoutId.isBlank()) {
                throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
            }
        }
    }
}