package br.com.supermercado.application.reporting;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.application.sales.SaleService.SalesSummary;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;
import br.com.supermercado.domain.product.Product;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Indicadores de atendimento: caixas abertos, clientes aguardando, total atendido,
 * espera média (geral e por caixa), caixa com mais atendimentos e menor fila.
 * Com os serviços de produtos, estoque e vendas, também monta a visão geral do
 * administrador (estoque, vendas do dia, mais vendidos e atendimento).
 */
public final class DashboardService {
    private static final int TOP_SELLING_IN_OVERVIEW = 3;

    private final QueueService queues;
    private final ProductService products;
    private final InventoryService inventory;
    private final SaleService sales;

    /** Apenas indicadores de atendimento. */
    public DashboardService(QueueService queues) {
        this(queues, null, null, null);
    }

    public DashboardService(QueueService queues, ProductService products, InventoryService inventory,
                            SaleService sales) {
        this.queues = Objects.requireNonNull(queues, "O serviço de filas é obrigatório.");
        this.products = products;
        this.inventory = inventory;
        this.sales = sales;
    }

    /** Visão geral do supermercado na data informada. */
    public StoreOverview storeOverview(LocalDate date) {
        if (products == null || inventory == null || sales == null) {
            throw new IllegalStateException("Visão geral indisponível: faltam os serviços de produtos, estoque e vendas.");
        }
        int outOfStock = 0;
        double stockValue = 0;
        List<Product> all = products.listAll();
        for (Product p : all) {
            if (p.quantity() == 0) outOfStock++;
            stockValue += p.quantity() * p.price();
        }
        return new StoreOverview(
                all.size(),
                inventory.listBelowMinimum().size(),
                outOfStock,
                stockValue,
                sales.summarize(sales.listByDate(date)),
                inventory.topSelling(TOP_SELLING_IN_OVERVIEW),
                queueOverview());
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

    public record StoreOverview(
            int activeProducts,
            int belowMinimum,
            int outOfStock,
            double stockValue,
            SalesSummary salesOfDay,
            List<TopSellingProduct> topSelling,
            QueueOverview queues) {
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
