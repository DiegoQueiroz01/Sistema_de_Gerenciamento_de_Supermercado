package br.com.supermercado;

import br.com.supermercado.application.auth.AuthService;
import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.application.store.SupermarketService;
import br.com.supermercado.domain.checkout.AttendanceQueue;
import br.com.supermercado.infrastructure.memory.InMemoryCheckoutRepository;
import br.com.supermercado.infrastructure.memory.InMemoryProductRepository;
import br.com.supermercado.infrastructure.memory.InMemorySaleRepository;
import br.com.supermercado.infrastructure.memory.InMemoryServiceRecordRepository;
import br.com.supermercado.infrastructure.memory.InMemoryStockMovementRepository;
import br.com.supermercado.infrastructure.memory.InMemorySupermarketRepository;
import br.com.supermercado.infrastructure.memory.InMemoryUserRepository;

// Junta todos os serviços do sistema, já ligados aos repositórios em memória
public class AppServices {
    private final AuthService auth;
    private final SupermarketService store;
    private final ProductService products;
    private final InventoryService inventory;
    private final SaleService sales;
    private final QueueService queues;
    private final DashboardService dashboard;

    private AppServices() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemorySaleRepository saleRepository = new InMemorySaleRepository();

        products = new ProductService(productRepository);
        inventory = new InventoryService(productRepository, new InMemoryStockMovementRepository(),
                SaleService.salesSource(saleRepository));
        sales = new SaleService(saleRepository, products, inventory);
        queues = new QueueService(new InMemoryCheckoutRepository(), new InMemoryServiceRecordRepository(),
                AttendanceQueue.DEFAULT_CAPACITY);
        auth = new AuthService(new InMemoryUserRepository());
        store = new SupermarketService(new InMemorySupermarketRepository());
        dashboard = new DashboardService(queues, products, inventory, sales);
    }

    public static AppServices inMemory() {
        return new AppServices();
    }

    public AuthService auth() { return auth; }
    public SupermarketService store() { return store; }
    public ProductService products() { return products; }
    public InventoryService inventory() { return inventory; }
    public SaleService sales() { return sales; }
    public QueueService queues() { return queues; }
    public DashboardService dashboard() { return dashboard; }
}
