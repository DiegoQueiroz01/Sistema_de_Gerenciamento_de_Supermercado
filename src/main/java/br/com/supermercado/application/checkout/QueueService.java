package br.com.supermercado.application.checkout;

import br.com.supermercado.domain.checkout.AttendanceQueue;
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
    private final int queueCapacity;
    private long nextCustomerId = 1;
    private long nextServiceRecordId = 1;
    private int nextCheckoutNumber = 1;

    public QueueService() {
        this(AttendanceQueue.DEFAULT_CAPACITY);
    }

    public QueueService(int queueCapacity) {
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("A capacidade da fila deve ser positiva.");
        }
        this.queueCapacity = queueCapacity;
    }

    /** Cria um caixa fechado com o próximo número livre ("1", "2", ...). */
    public Checkout createCheckout() {
        while (findCheckout(String.valueOf(nextCheckoutNumber)).isPresent()) {
            nextCheckoutNumber++;
        }
        Checkout checkout = new Checkout(String.valueOf(nextCheckoutNumber++), queueCapacity);
        checkouts.add(checkout);
        return checkout;
    }

    public Checkout openCheckout(String checkoutId) {
        Checkout checkout = findCheckout(checkoutId)
                .orElseGet(() -> {
                    Checkout newCheckout = new Checkout(checkoutId, queueCapacity);
                    checkouts.add(newCheckout);
                    return newCheckout;
                });
        checkout.open();
        return checkout;
    }

    /**
     * Fecha o caixa. Um atendimento em andamento é concluído e os clientes aguardando
     * são redistribuídos para as menores filas abertas; se não couberem, o fechamento é recusado.
     */
    public void closeCheckout(String checkoutId) {
        Checkout checkout = requireCheckout(checkoutId);
        if (checkout.status() != CheckoutStatus.OPEN) {
            throw new IllegalStateException("O caixa já está fechado: " + checkoutId);
        }

        int waiting = checkout.waitingCount();
        if (waiting > 0) {
            if (checkouts.stream().noneMatch(other ->
                    other != checkout && other.status() == CheckoutStatus.OPEN)) {
                throw new IllegalStateException(
                        "Abra outro caixa antes de fechar este: há clientes aguardando.");
            }
            if (freeSlotsExcluding(checkout) < waiting) {
                throw new IllegalStateException(
                        "Os outros caixas abertos não têm espaço para os clientes aguardando.");
            }
        }

        checkout.finishService();
        while (checkout.waitingCount() > 0) {
            Customer customer = checkout.removeNextCustomer().orElseThrow();
            Checkout destination = findShortestOpenCheckout(checkout)
                    .orElseThrow(() -> new IllegalStateException(
                            "Não há outro caixa aberto para receber os clientes."));
            destination.enqueueByArrival(customer);
        }
        checkout.close();
    }

    /** Remove um caixa fechado e sem clientes. */
    public void removeCheckout(String checkoutId) {
        Checkout checkout = requireCheckout(checkoutId);
        if (checkout.isOpen()) {
            throw new IllegalStateException("Feche o caixa antes de removê-lo: " + checkoutId);
        }
        checkouts.remove(checkout);
    }

    public CustomerAssignment registerArrival(boolean priority, LocalDateTime arrivalTime) {
        return registerArrival(Customer.DEFAULT_NAME, priority, arrivalTime);
    }

    /** Registra a chegada com senha sequencial e direciona o cliente para a menor fila com espaço. */
    public CustomerAssignment registerArrival(String name, boolean priority,
                                              LocalDateTime arrivalTime) {
        Objects.requireNonNull(arrivalTime, "O horário de chegada é obrigatório.");
        Checkout checkout = findShortestOpenCheckout(null)
                .orElseThrow(() -> new IllegalStateException(
                        "Não há caixas abertos com espaço para receber clientes."));

        long id = nextCustomerId++;
        Customer customer = new Customer(id, Customer.formatTicket(id), name, priority, arrivalTime);
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
        if (destination.isQueueFull()) {
            throw new IllegalStateException(
                    "A fila do caixa de destino está cheia: " + destinationCheckoutId);
        }

        Customer customer = source.removeCustomer(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O cliente não está na fila de origem: " + customerId));
        destination.enqueueByArrival(customer);
    }

    /** Transfere pela senha do cliente (ex.: "S003"). */
    public void transferCustomer(String ticket, String sourceCheckoutId,
                                 String destinationCheckoutId) {
        Customer customer = requireCheckout(sourceCheckoutId).findWaitingByTicket(ticket)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Senha não encontrada na fila de origem: " + ticket));
        transferCustomer(customer.id(), sourceCheckoutId, destinationCheckoutId);
    }

    /**
     * Chama o próximo cliente da fila. Se houver alguém em atendimento, esse atendimento
     * é concluído primeiro. Registra o início do atendimento para o cálculo da espera.
     */
    public Optional<ServiceRecord> startNextAttendance(
            String checkoutId, LocalDateTime serviceStartedAt) {
        Objects.requireNonNull(serviceStartedAt, "O horário de atendimento é obrigatório.");
        Checkout checkout = requireCheckout(checkoutId);
        Customer customer = checkout.peekNextCustomer().orElse(null);
        checkout.finishService();
        if (customer == null) {
            return Optional.empty();
        }

        ServiceRecord record = new ServiceRecord(
                nextServiceRecordId++,
                checkout.id(),
                customer,
                serviceStartedAt);
        checkout.removeNextCustomer().orElseThrow();
        checkout.startService(customer);
        serviceRecords.add(record);
        return Optional.of(record);
    }

    /** Conclui o atendimento em andamento no caixa, sem chamar o próximo. */
    public Optional<Customer> finishAttendance(String checkoutId) {
        return requireCheckout(checkoutId).finishService();
    }

    public Optional<Checkout> checkout(String checkoutId) {
        return findCheckout(checkoutId);
    }

    /** Caixa aberto com a menor fila e espaço disponível. */
    public Optional<Checkout> shortestOpenCheckout() {
        return findShortestOpenCheckout(null);
    }

    public List<Checkout> checkouts() {
        return List.copyOf(checkouts);
    }

    public List<Checkout> openCheckouts() {
        return checkouts.stream().filter(Checkout::isOpen).toList();
    }

    public List<ServiceRecord> serviceRecords() {
        return List.copyOf(serviceRecords);
    }

    public List<ServiceRecord> serviceRecordsOf(String checkoutId) {
        requireCheckout(checkoutId);
        return serviceRecords.stream()
                .filter(record -> record.checkoutId().equals(checkoutId))
                .toList();
    }

    public int totalWaiting() {
        return checkouts.stream().mapToInt(Checkout::waitingCount).sum();
    }

    /** Média de clientes aguardando por caixa aberto. */
    public double averageLoad() {
        int open = openCheckouts().size();
        return open == 0 ? 0 : (double) totalWaiting() / open;
    }

    private int freeSlotsExcluding(Checkout excludedCheckout) {
        return checkouts.stream()
                .filter(checkout -> checkout != excludedCheckout && checkout.isOpen())
                .mapToInt(Checkout::freeSlots)
                .sum();
    }

    private Optional<Checkout> findShortestOpenCheckout(Checkout excludedCheckout) {
        return checkouts.stream()
                .filter(checkout -> checkout != excludedCheckout)
                .filter(checkout -> checkout.status() == CheckoutStatus.OPEN)
                .filter(checkout -> !checkout.isQueueFull())
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
                .filter(checkout -> checkout.id().equals(checkoutId.trim()))
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
