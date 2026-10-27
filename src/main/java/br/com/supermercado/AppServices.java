package br.com.supermercado;

import br.com.supermercado.application.auth.AuthService;
import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.infrastructure.memory.InMemoryProductRepository;
import br.com.supermercado.infrastructure.memory.InMemorySaleRepository;
import br.com.supermercado.infrastructure.memory.InMemoryStockMovementRepository;
import br.com.supermercado.infrastructure.memory.InMemoryUserRepository;
import br.com.supermercado.infrastructure.mysql.MySQLProductRepository;
import br.com.supermercado.infrastructure.mysql.MySQLStockMovementRepository;
import br.com.supermercado.repositories.ProductRepository;
import br.com.supermercado.repositories.StockMovementRepository;

/** Serviços da aplicação já ligados aos repositórios escolhidos. */
public record AppServices(
        AuthService auth,
        ProductService products,
        InventoryService inventory,
        SaleService sales,
        QueueService queues,
        DashboardService dashboard) {

    /** Tudo em memória (estruturas de dados), sem banco de dados. */
    public static AppServices inMemory() {
        return create(new InMemoryProductRepository(), new InMemoryStockMovementRepository());
    }

    /** Produtos, categorias e movimentações de estoque no MySQL; o restante em memória. */
    public static AppServices withMySQL() {
        return create(new MySQLProductRepository(), new MySQLStockMovementRepository());
    }

    private static AppServices create(ProductRepository productRepository,
                                      StockMovementRepository movementRepository) {
        InMemorySaleRepository saleRepository = new InMemorySaleRepository();
        ProductService products = new ProductService(productRepository);
        InventoryService inventory = new InventoryService(productRepository, movementRepository,
                SaleService.salesSource(saleRepository));
        SaleService sales = new SaleService(saleRepository, products, inventory);
        QueueService queues = new QueueService();
        return new AppServices(
                new AuthService(new InMemoryUserRepository()),
                products,
                inventory,
                sales,
                queues,
                new DashboardService(queues));
    }
}
