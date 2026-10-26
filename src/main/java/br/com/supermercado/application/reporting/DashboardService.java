package br.com.supermercado.application.reporting;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Indicadores de atendimento: caixas abertos, clientes aguardando, total atendido,
 * espera média (geral e por caixa), caixa com mais atendimentos e menor fila.
 * As métricas de estoque (mais vendidos) ficam no InventoryService.
 */
public final class DashboardService {
    private final QueueService queues;

    public DashboardService(QueueService queues) {
        this.queues = Objects.requireNonNull(queues, "O serviço de filas é obrigatório.");
    }

    public QueueOverview queueOverview() {
        List<Checkout> checkouts = queues.checkouts();
        List<ServiceRecord> records = queues.serviceRecords();

        List<CheckoutSummary> summaries = checkouts.stream()
                .map(this::summarize)
                .toList();
        Optional<String> busiest = summaries.stream()
                .filter(summary -> summary.attendances() > 0)
                .max(Comparator.comparingInt(CheckoutSummary::attendances))
                .map(CheckoutSummary::checkoutId);
        Optional<String> shortest = queues.shortestOpenCheckout().map(Checkout::id);

        return new QueueOverview(
                checkouts.size(),
                queues.openCheckouts().size(),
                queues.totalWaiting(),
                records.size(),
                averageWait(records),
                busiest,
                shortest,
                summaries);
    }

    public Duration averageWait(List<ServiceRecord> records) {
        if (records.isEmpty()) {
            return Duration.ZERO;
        }
        Duration total = records.stream()
                .map(ServiceRecord::calculateWaitTime)
                .reduce(Duration.ZERO, Duration::plus);
        return total.dividedBy(records.size());
    }

    private CheckoutSummary summarize(Checkout checkout) {
        List<ServiceRecord> records = queues.serviceRecordsOf(checkout.id());
        return new CheckoutSummary(
                checkout.id(),
                checkout.isOpen(),
                checkout.waitingCount(),
                records.size(),
                averageWait(records),
                checkout.customerInService());
    }

    public record QueueOverview(
            int totalCheckouts,
            int openCheckouts,
            int waitingCustomers,
            int totalAttendances,
            Duration averageWait,
            Optional<String> busiestCheckoutId,
            Optional<String> shortestQueueCheckoutId,
            List<CheckoutSummary> checkouts) {
    }

    public record CheckoutSummary(
            String checkoutId,
            boolean open,
            int waiting,
            int attendances,
            Duration averageWait,
            Optional<Customer> customerInService) {
    }
}
