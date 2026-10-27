package br.com.supermercado;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.reporting.DashboardService.StoreOverview;
import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.product.Category;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Testes de saída manual, desativação de produto, visão geral e parâmetros.
 * Execução: mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.AdminTests -Dexec.classpathScope=test
 */
public class AdminTests {
    static int passed = 0, failed = 0;

    interface Action { void run() throws Exception; }

    static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("PASSOU  " + name); }
        else { failed++; System.out.println("FALHOU  " + name); }
    }

    static boolean throwsType(Class<? extends Exception> type, Action action) {
        try { action.run(); return false; } catch (Exception e) { return type.isInstance(e); }
    }

    static int run() {
        passed = 0;
        failed = 0;
        AppServices app = AppServices.inMemory();
        ProductService products = app.products();
        InventoryService inventory = app.inventory();
        LocalDateTime now = LocalDateTime.now();
        Category grocery = products.registerCategory("Mercearia");
        products.registerProduct("A", "Arroz", grocery, 20.0, 5, now);
        products.registerProduct("F", "Feijão", grocery, 10.0, 5, now);
        products.registerProduct("S", "Sal", grocery, 2.0, 0, now);
        inventory.registerEntry("A", 10, "t");
        inventory.registerEntry("F", 3, "t");

        // Saída manual
        inventory.registerExit("A", 2, "Carlos");
        check("CA01 saída manual baixa estoque e entra no histórico",
                products.requireByCode("A").quantity() == 8
                        && inventory.historyOf("A").get(0).type() == MovementType.EXIT
                        && inventory.historyOf("A").get(0).responsible().equals("Carlos"));

        // Desativação
        check("CA02 desativar com estoque é recusado", throwsType(IllegalStateException.class,
                () -> products.deactivate("F")) && products.findByCode("F") != null);
        inventory.registerExit("F", 3, "Carlos");
        products.deactivate("F");
        check("CA03 produto desativado sai das consultas", products.findByCode("F") == null
                && products.listAll().size() == 2 && products.searchByName("feij").isEmpty());
        check("CA04 histórico do desativado é mantido", inventory.historyOf("F").size() == 2);
        check("CA05 desativado não pode ser vendido", throwsType(IllegalArgumentException.class,
                () -> app.sales().newCart().add("F", 1)));
        check("CA06 desativar produto inexistente recusado", throwsType(IllegalArgumentException.class,
                () -> products.deactivate("X")));
        products.registerProduct("F", "Feijão Preto", grocery, 11.0, 5, now);
        check("CA07 cadastrar o mesmo código reativa o produto", products.requireByCode("F").name().equals("Feijão Preto")
                && products.requireByCode("F").quantity() == 0);

        // Visão geral
        Cart cart = app.sales().newCart();
        cart.add("A", 3);
        app.sales().finalizeSale(cart, "Marta", null, null, now);
        DemoData.loadCheckouts(app);
        app.queues().registerArrival("Ana", false, now);
        StoreOverview o = app.dashboard().storeOverview(LocalDate.now());
        check("CA08 visão geral: produtos, alertas e sem estoque",
                o.activeProducts() == 3 && o.belowMinimum() == 1 && o.outOfStock() == 2);
        check("CA09 visão geral: valor em estoque", o.stockValue() == 5 * 20.0);
        check("CA10 visão geral: vendas do dia", o.salesOfDay().sales() == 1
                && o.salesOfDay().revenue() == 60.0 && o.topSelling().get(0).unitsSold() == 3);
        check("CA11 visão geral: atendimento", o.queues().openCheckouts() == 3 && o.queues().waitingCustomers() == 1);
        check("CA12 visão geral de outro dia não tem vendas",
                app.dashboard().storeOverview(LocalDate.now().minusDays(1)).salesOfDay().sales() == 0);

        // Parâmetros
        app.queues().changeQueueCapacity(2);
        Checkout novo = app.queues().createCheckout();
        check("CA13 nova capacidade vale para caixas novos", novo.queueCapacity() == 2
                && app.queues().checkout("1").queueCapacity() != 2);
        check("CA14 capacidade inválida recusada", throwsType(IllegalArgumentException.class,
                () -> app.queues().changeQueueCapacity(0)) && app.queues().queueCapacity() == 2);

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        return failed;
    }

    public static void main(String[] args) {
        System.exit(run() == 0 ? 0 : 1);
    }
}
