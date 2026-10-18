package br.com.supermercado;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.SalesSource;
import br.com.supermercado.application.inventory.InventoryService.SoldItem;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.infrastructure.memory.InMemoryProductRepository;
import br.com.supermercado.infrastructure.memory.InMemoryStockMovementRepository;
import br.com.supermercado.presentation.cli.ProductAdminMenu;
import br.com.supermercado.presentation.cli.StockistMenu;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Execução isolada do módulo E.1 (demonstração e testes), SEM login e SEM código do E.2.
 * Fica em src/test para não fazer parte da arquitetura de produção.
 *
 * Executar: java -cp target/classes:target/test-classes br.com.supermercado.E1Demo
 */
public final class E1Demo {

    /** Simula a fonte de vendas que o E.2 fornecerá (implementação de InventoryService.SalesSource). */
    static final class FakeSales implements SalesSource {
        private final List<SoldItem> items = new ArrayList<>();

        void add(List<SoldItem> sold) { items.addAll(sold); }

        @Override
        public List<SoldItem> findAllSoldItems() { return List.copyOf(items); }
    }

    /** Conjunto de serviços já ligados e preenchidos com dados de demonstração. */
    record Setup(ProductService products, InventoryService inventory, FakeSales sales) { }

    static Setup createSeededSetup() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryStockMovementRepository movementRepository = new InMemoryStockMovementRepository();
        FakeSales sales = new FakeSales();
        ProductService products = new ProductService(productRepository);
        InventoryService inventory = new InventoryService(productRepository, movementRepository, sales);

        Category beverages = products.registerCategory("Bebidas");
        Category dairy = products.registerCategory("Laticínios");
        Category produce = products.registerCategory("Hortifruti");
        Category cleaning = products.registerCategory("Limpeza");
        products.registerCategory("Padaria");
        Category grocery = products.registerCategory("Mercearia");

        LocalDateTime recent = LocalDateTime.now().minusDays(2);
        String who = "Carlos Oliveira";
        // quantidade inicial = quantidade final + unidades vendidas nas vendas abaixo
        add(products, inventory, "78910001", "Refrigerante Coca-Cola 2L", beverages, 8.99, 60, 20, recent, who);
        add(products, inventory, "78910002", "Leite Integral Aurora 1L", dairy, 4.20, 33, 10, recent, who);
        add(products, inventory, "78910003", "Manteiga Aviação com Sal 200g", dairy, 9.50, 11, 12, recent, who);
        add(products, inventory, "78910004", "Detergente Ypê Coco 500ml", cleaning, 2.20, 18, 10, recent, who);
        add(products, inventory, "78910005", "Arroz Agulhinha Tipo 1 Tio João 5kg", grocery, 24.90, 27, 15, recent, who);
        add(products, inventory, "78910006", "Detergente Líquido Limpol 500ml", cleaning, 3.80, 8, 20, recent, who);
        add(products, inventory, "78910011", "Feijão Carioca 1kg", grocery, 7.50, 120, 20, recent, who);
        add(products, inventory, "78910012", "Banana Prata (kg)", produce, 5.99, 40, 10, recent, who);
        add(products, inventory, "78910007", "Azeite de Oliva Extra Virgem 500ml", grocery, 39.90, 42, 10,
                LocalDateTime.now().minusDays(34), who);
        add(products, inventory, "78910008", "Sabão em Pó Omo Multiação 1kg", cleaning, 14.50, 60, 10,
                LocalDateTime.now().minusDays(45), who);
        add(products, inventory, "78910009", "Cerveja Heineken Lata 350ml", beverages, 5.49, 110, 24,
                LocalDateTime.now().minusDays(31), who);
        add(products, inventory, "78910010", "Saco de Lixo Embalixo 50L", cleaning, 12.00, 25, 10,
                LocalDateTime.now().minusDays(52), who);

        sell(inventory, sales, LocalDateTime.now().minusHours(3),
                new SoldItem("78910002", 20), new SoldItem("78910005", 15));
        sell(inventory, sales, LocalDateTime.now().minusHours(1),
                new SoldItem("78910002", 10), new SoldItem("78910005", 10),
                new SoldItem("78910011", 20), new SoldItem("78910001", 15));
        return new Setup(products, inventory, sales);
    }

    private static void add(ProductService products, InventoryService inventory, String code, String name,
                            Category category, double price, int quantity, int minimum,
                            LocalDateTime date, String who) {
        products.registerProduct(code, name, category, price, minimum, date);
        inventory.registerEntry(code, quantity, who, date);
    }

    private static void sell(InventoryService inventory, FakeSales sales, LocalDateTime date, SoldItem... items) {
        inventory.registerSaleExit(date, List.of(items), "Marta Rocha");
        sales.add(List.of(items));
    }

    public static void main(String[] args) {
        Charset consoleCharset = Charset.forName(System.getProperty("stdout.encoding", Charset.defaultCharset().name()));
PrintStream out = System.out;
Scanner scanner = new Scanner(System.in, consoleCharset);
        Setup s = createSeededSetup();
        try {
            out.println("MERCADO FÁCIL - Módulo E.1 (Controle de Produtos e Estoque) - modo demonstração");
            int option;
            do {
                out.println("\n=== PERFIL (demonstração, sem login) ===");
                out.println("  1) Estoquista (Carlos Oliveira)");
                out.println("  2) Administrador - produtos e estoque (Juliana Silva)");
                out.println("  0) Sair");
                out.print("Escolha: ");
                String text = scanner.nextLine().trim();
                option = text.equals("1") ? 1 : text.equals("2") ? 2 : text.equals("0") ? 0 : -1;
                if (option == 1) {
                    new StockistMenu(scanner, out, s.products(), s.inventory(), "Carlos Oliveira").run();
                } else if (option == 2) {
                    new ProductAdminMenu(scanner, out, s.products(), s.inventory(), "Juliana Silva").run();
                } else if (option == -1) {
                    out.println("  [ERRO] Opção inválida.");
                }
            } while (option != 0);
            out.println("\nSessão encerrada. Até logo!");
        } catch (NoSuchElementException e) {
            out.println("\nEntrada encerrada. Programa finalizado.");
        }
    }
}
