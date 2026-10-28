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
import java.util.ArrayList;
import java.util.List;

// Números para a visão geral: caixas, filas, tempo de espera,
// e (para o administrador) estoque e vendas do dia.
public class DashboardService {
    private static final int TOP_SELLING_IN_OVERVIEW = 3;

    // Resumo de um caixa
    public static class CheckoutSummary {
        private final String checkoutId;
        private final boolean open;
        private final int waiting;
        private final int attendances;
        private final Duration averageWait;
        private final Customer customerInService; // pode ser null

        public CheckoutSummary(String checkoutId, boolean open, int waiting, int attendances,
                               Duration averageWait, Customer customerInService) {
            this.checkoutId = checkoutId;
            this.open = open;
            this.waiting = waiting;
            this.attendances = attendances;
            this.averageWait = averageWait;
            this.customerInService = customerInService;
        }

        public String checkoutId() { return checkoutId; }
        public boolean open() { return open; }
        public int waiting() { return waiting; }
        public int attendances() { return attendances; }
        public Duration averageWait() { return averageWait; }
        public Customer customerInService() { return customerInService; }
    }

    // Resumo de todos os caixas
    public static class QueueOverview {
        private final int totalCheckouts;
        private final int openCheckouts;
        private final int waitingCustomers;
        private final int totalAttendances;
        private final Duration averageWait;
        private final String busiestCheckoutId;       // null se ninguém foi atendido
        private final String shortestQueueCheckoutId; // null se não há caixa aberto com espaço
        private final List<CheckoutSummary> checkouts;

        public QueueOverview(int totalCheckouts, int openCheckouts, int waitingCustomers,
                             int totalAttendances, Duration averageWait, String busiestCheckoutId,
                             String shortestQueueCheckoutId, List<CheckoutSummary> checkouts) {
            this.totalCheckouts = totalCheckouts;
            this.openCheckouts = openCheckouts;
            this.waitingCustomers = waitingCustomers;
            this.totalAttendances = totalAttendances;
            this.averageWait = averageWait;
            this.busiestCheckoutId = busiestCheckoutId;
            this.shortestQueueCheckoutId = shortestQueueCheckoutId;
            this.checkouts = checkouts;
        }

        public int totalCheckouts() { return totalCheckouts; }
        public int openCheckouts() { return openCheckouts; }
        public int waitingCustomers() { return waitingCustomers; }
        public int totalAttendances() { return totalAttendances; }
        public Duration averageWait() { return averageWait; }
        public String busiestCheckoutId() { return busiestCheckoutId; }
        public String shortestQueueCheckoutId() { return shortestQueueCheckoutId; }
        public List<CheckoutSummary> checkouts() { return checkouts; }
    }

    // Visão geral do supermercado (tela do administrador)
    public static class StoreOverview {
        private final int activeProducts;
        private final int belowMinimum;
        private final int outOfStock;
        private final double stockValue;
        private final SalesSummary salesOfDay;
        private final List<TopSellingProduct> topSelling;
        private final QueueOverview queues;

        public StoreOverview(int activeProducts, int belowMinimum, int outOfStock, double stockValue,
                             SalesSummary salesOfDay, List<TopSellingProduct> topSelling,
                             QueueOverview queues) {
            this.activeProducts = activeProducts;
            this.belowMinimum = belowMinimum;
            this.outOfStock = outOfStock;
            this.stockValue = stockValue;
            this.salesOfDay = salesOfDay;
            this.topSelling = topSelling;
            this.queues = queues;
        }

        public int activeProducts() { return activeProducts; }
        public int belowMinimum() { return belowMinimum; }
        public int outOfStock() { return outOfStock; }
        public double stockValue() { return stockValue; }
        public SalesSummary salesOfDay() { return salesOfDay; }
        public List<TopSellingProduct> topSelling() { return topSelling; }
        public QueueOverview queues() { return queues; }
    }

    private final QueueService queues;
    private final ProductService products;
    private final InventoryService inventory;
    private final SaleService sales;

    // Só com os caixas (usado na demonstração do módulo de filas)
    public DashboardService(QueueService queues) {
        this(queues, null, null, null);
    }

    public DashboardService(QueueService queues, ProductService products, InventoryService inventory,
                            SaleService sales) {
        this.queues = queues;
        this.products = products;
        this.inventory = inventory;
        this.sales = sales;
    }

    public StoreOverview storeOverview(LocalDate date) {
        if (products == null || inventory == null || sales == null) {
            throw new IllegalStateException("Visão geral indisponível: faltam os serviços de produtos, estoque e vendas.");
        }
        int outOfStock = 0;
        double stockValue = 0;
        List<Product> all = products.listAll();
        for (Product p : all) {
            if (p.quantity() == 0) {
                outOfStock++;
            }
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

        List<CheckoutSummary> summaries = new ArrayList<>();
        for (Checkout c : checkouts) {
            summaries.add(summarize(c));
        }

        // caixa com mais atendimentos (se empatar, fica o primeiro)
        String busiest = null;
        int most = 0;
        for (CheckoutSummary s : summaries) {
            if (s.attendances() > most) {
                most = s.attendances();
                busiest = s.checkoutId();
            }
        }

        String shortest = null;
        Checkout shortestCheckout = queues.shortestOpenCheckout();
        if (shortestCheckout != null) {
            shortest = shortestCheckout.id();
        }

        return new QueueOverview(checkouts.size(), queues.openCheckouts().size(), queues.totalWaiting(),
                records.size(), averageWait(records), busiest, shortest, summaries);
    }

    public Duration averageWait(List<ServiceRecord> records) {
        if (records.isEmpty()) {
            return Duration.ZERO;
        }
        Duration total = Duration.ZERO;
        for (ServiceRecord r : records) {
            total = total.plus(r.calculateWaitTime());
        }
        return total.dividedBy(records.size());
    }

    private CheckoutSummary summarize(Checkout checkout) {
        List<ServiceRecord> records = queues.serviceRecordsOf(checkout.id());
        return new CheckoutSummary(checkout.id(), checkout.isOpen(), checkout.waitingCount(),
                records.size(), averageWait(records), checkout.customerInService());
    }
}
