package br.com.supermercado.application.checkout;

import br.com.supermercado.domain.checkout.AttendanceQueue;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;
import br.com.supermercado.infrastructure.memory.InMemoryCheckoutRepository;
import br.com.supermercado.infrastructure.memory.InMemoryServiceRecordRepository;
import br.com.supermercado.repositories.CheckoutRepository;
import br.com.supermercado.repositories.ServiceRecordRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Operações de caixas e filas: abrir/fechar caixa, chegada de cliente,
// atendimento, transferência e histórico.
public class QueueService {

    // Resultado da chegada de um cliente: o cliente criado e o caixa para onde ele foi
    public static class CustomerAssignment {
        private final Customer customer;
        private final String checkoutId;

        public CustomerAssignment(Customer customer, String checkoutId) {
            this.customer = customer;
            this.checkoutId = checkoutId;
        }

        public Customer customer() { return customer; }
        public String checkoutId() { return checkoutId; }
    }

    private final CheckoutRepository checkoutRepository;
    private final ServiceRecordRepository recordRepository;
    private int queueCapacity;
    private long nextCustomerId = 1;
    private int nextCheckoutNumber = 1;

    public QueueService() {
        this(AttendanceQueue.DEFAULT_CAPACITY);
    }

    public QueueService(int queueCapacity) {
        this(new InMemoryCheckoutRepository(), new InMemoryServiceRecordRepository(), queueCapacity);
    }

    public QueueService(CheckoutRepository checkoutRepository, ServiceRecordRepository recordRepository,
                        int queueCapacity) {
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("A capacidade da fila deve ser positiva.");
        }
        this.checkoutRepository = checkoutRepository;
        this.recordRepository = recordRepository;
        this.queueCapacity = queueCapacity;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    // Parâmetro do administrador: só vale para os caixas criados depois
    public void changeQueueCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A capacidade da fila deve ser positiva.");
        }
        this.queueCapacity = capacity;
    }

    // Cria um caixa fechado com o próximo número livre ("1", "2", ...)
    public Checkout createCheckout() {
        while (findCheckout(String.valueOf(nextCheckoutNumber)) != null) {
            nextCheckoutNumber++;
        }
        Checkout checkout = new Checkout(String.valueOf(nextCheckoutNumber), queueCapacity);
        nextCheckoutNumber++;
        checkoutRepository.save(checkout);
        return checkout;
    }

    public Checkout openCheckout(String checkoutId) {
        Checkout checkout = findCheckout(checkoutId);
        if (checkout == null) {
            checkout = new Checkout(checkoutId.trim(), queueCapacity);
        }
        checkout.open();
        checkoutRepository.save(checkout);
        return checkout;
    }

    // Fecha o caixa. Se tiver alguém sendo atendido, o atendimento termina.
    // Quem estava na fila vai para as menores filas dos outros caixas abertos.
    // Se não couber todo mundo, o caixa não fecha.
    public void closeCheckout(String checkoutId) {
        Checkout checkout = getCheckout(checkoutId);
        if (!checkout.isOpen()) {
            throw new IllegalStateException("O caixa já está fechado: " + checkoutId);
        }

        int waiting = checkout.waitingCount();
        if (waiting > 0) {
            boolean hasOtherOpen = false;
            int freeSlots = 0;
            for (Checkout other : checkouts()) {
                if (other != checkout && other.isOpen()) {
                    hasOtherOpen = true;
                    freeSlots += other.freeSlots();
                }
            }
            if (!hasOtherOpen) {
                throw new IllegalStateException("Abra outro caixa antes de fechar este: há clientes aguardando.");
            }
            if (freeSlots < waiting) {
                throw new IllegalStateException("Os outros caixas abertos não têm espaço para os clientes aguardando.");
            }
        }

        checkout.finishService();
        while (checkout.waitingCount() > 0) {
            Customer customer = checkout.removeNextCustomer();
            Checkout destination = findShortestOpenCheckout(checkout);
            destination.enqueueByArrival(customer);
            checkoutRepository.save(destination);
        }
        checkout.close();
        checkoutRepository.save(checkout);
    }

    // Só remove caixa fechado (e caixa fechado não tem fila)
    public void removeCheckout(String checkoutId) {
        Checkout checkout = getCheckout(checkoutId);
        if (checkout.isOpen()) {
            throw new IllegalStateException("Feche o caixa antes de removê-lo: " + checkoutId);
        }
        checkoutRepository.remove(checkout.id());
    }

    public CustomerAssignment registerArrival(boolean priority, LocalDateTime arrivalTime) {
        return registerArrival(Customer.DEFAULT_NAME, priority, arrivalTime);
    }

    // Gera a senha do cliente e coloca ele na menor fila que ainda tem espaço
    public CustomerAssignment registerArrival(String name, boolean priority, LocalDateTime arrivalTime) {
        if (arrivalTime == null) {
            throw new IllegalArgumentException("O horário de chegada é obrigatório.");
        }
        Checkout checkout = findShortestOpenCheckout(null);
        if (checkout == null) {
            throw new IllegalStateException("Não há caixas abertos com espaço para receber clientes.");
        }

        long id = nextCustomerId;
        nextCustomerId++;
        Customer customer = new Customer(id, Customer.formatTicket(id), name, priority, arrivalTime);
        checkout.enqueue(customer);
        checkoutRepository.save(checkout);
        return new CustomerAssignment(customer, checkout.id());
    }

    public void transferCustomer(long customerId, String sourceCheckoutId, String destinationCheckoutId) {
        Checkout source = getCheckout(sourceCheckoutId);
        Checkout destination = getCheckout(destinationCheckoutId);
        if (sourceCheckoutId.equals(destinationCheckoutId)) {
            throw new IllegalArgumentException("A origem e o destino devem ser diferentes.");
        }
        if (!source.isOpen() || !destination.isOpen()) {
            throw new IllegalStateException("Origem e destino devem estar abertos.");
        }
        if (destination.hasCustomer(customerId)) {
            throw new IllegalArgumentException("O cliente já está na fila de destino: " + customerId);
        }
        if (destination.isQueueFull()) {
            throw new IllegalStateException("A fila do caixa de destino está cheia: " + destinationCheckoutId);
        }

        Customer customer = source.removeCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("O cliente não está na fila de origem: " + customerId);
        }
        destination.enqueueByArrival(customer);
        checkoutRepository.save(source);
        checkoutRepository.save(destination);
    }

    // Transferência usando a senha do cliente (ex.: "S003")
    public void transferCustomer(String ticket, String sourceCheckoutId, String destinationCheckoutId) {
        Customer customer = getCheckout(sourceCheckoutId).findWaitingByTicket(ticket);
        if (customer == null) {
            throw new IllegalArgumentException("Senha não encontrada na fila de origem: " + ticket);
        }
        transferCustomer(customer.id(), sourceCheckoutId, destinationCheckoutId);
    }

    // Chama o próximo da fila. Se já tinha alguém sendo atendido, esse atendimento termina antes.
    // Devolve o registro do atendimento ou null se a fila estiver vazia.
    public ServiceRecord startNextAttendance(String checkoutId, LocalDateTime serviceStartedAt) {
        if (serviceStartedAt == null) {
            throw new IllegalArgumentException("O horário de atendimento é obrigatório.");
        }
        Checkout checkout = getCheckout(checkoutId);
        Customer customer = checkout.peekNextCustomer();
        checkout.finishService();
        if (customer == null) {
            checkoutRepository.save(checkout);
            return null;
        }

        ServiceRecord record = new ServiceRecord(recordRepository.nextId(), checkout.id(), customer,
                serviceStartedAt);
        checkout.removeNextCustomer();
        checkout.startService(customer);
        checkoutRepository.save(checkout);
        recordRepository.save(record);
        return record;
    }

    // Termina o atendimento atual sem chamar o próximo. Devolve o cliente atendido ou null.
    public Customer finishAttendance(String checkoutId) {
        Checkout checkout = getCheckout(checkoutId);
        Customer finished = checkout.finishService();
        checkoutRepository.save(checkout);
        return finished;
    }

    // Devolve null se o caixa não existir
    public Checkout checkout(String checkoutId) {
        return findCheckout(checkoutId);
    }

    // Caixa aberto com a menor fila e com espaço (null se não houver)
    public Checkout shortestOpenCheckout() {
        return findShortestOpenCheckout(null);
    }

    public List<Checkout> checkouts() {
        return checkoutRepository.findAll();
    }

    public List<Checkout> openCheckouts() {
        List<Checkout> open = new ArrayList<>();
        for (Checkout c : checkouts()) {
            if (c.isOpen()) {
                open.add(c);
            }
        }
        return open;
    }

    public List<ServiceRecord> serviceRecords() {
        return recordRepository.findAll();
    }

    public List<ServiceRecord> serviceRecordsOf(String checkoutId) {
        return recordRepository.findByCheckoutId(getCheckout(checkoutId).id());
    }

    public int totalWaiting() {
        int total = 0;
        for (Checkout c : checkouts()) {
            total += c.waitingCount();
        }
        return total;
    }

    // Média de clientes esperando por caixa aberto
    public double averageLoad() {
        int open = openCheckouts().size();
        if (open == 0) {
            return 0;
        }
        return (double) totalWaiting() / open;
    }

    // Percorre os caixas e guarda o aberto com menos gente na fila.
    // O caixa "ignored" fica de fora (usado ao fechar um caixa).
    private Checkout findShortestOpenCheckout(Checkout ignored) {
        Checkout shortest = null;
        for (Checkout c : checkouts()) {
            if (c == ignored || !c.isOpen() || c.isQueueFull()) {
                continue;
            }
            if (shortest == null || c.waitingCount() < shortest.waitingCount()) {
                shortest = c;
            }
        }
        return shortest;
    }

    private Checkout getCheckout(String checkoutId) {
        Checkout checkout = findCheckout(checkoutId);
        if (checkout == null) {
            throw new IllegalArgumentException("Caixa não encontrado: " + checkoutId);
        }
        return checkout;
    }

    private Checkout findCheckout(String checkoutId) {
        if (checkoutId == null || checkoutId.isBlank()) {
            throw new IllegalArgumentException("O identificador do caixa é obrigatório.");
        }
        return checkoutRepository.findById(checkoutId.trim());
    }
}
