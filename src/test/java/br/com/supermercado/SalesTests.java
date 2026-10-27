package br.com.supermercado;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.infrastructure.memory.InMemoryProductRepository;
import br.com.supermercado.infrastructure.memory.InMemorySaleRepository;
import br.com.supermercado.infrastructure.memory.InMemoryStockMovementRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Testes de vendas sem dependências externas.
 * Execução: mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.SalesTests -Dexec.classpathScope=test
 */
public class SalesTests {
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
        InMemoryProductRepository productRepo = new InMemoryProductRepository();
        InMemorySaleRepository saleRepo = new InMemorySaleRepository();
        ProductService products = new ProductService(productRepo);
        InventoryService inventory = new InventoryService(productRepo, new InMemoryStockMovementRepository(),
                SaleService.salesSource(saleRepo));
        SaleService sales = new SaleService(saleRepo, products, inventory);

        LocalDateTime now = LocalDateTime.now();
        Category grocery = products.registerCategory("Mercearia");
        products.registerProduct("A", "Arroz 5kg", grocery, 25.0, 2, now);
        products.registerProduct("F", "Feijão 1kg", grocery, 8.0, 2, now);
        inventory.registerEntry("A", 10, "t");
        inventory.registerEntry("F", 5, "t");

        // Carrinho
        Cart cart = sales.newCart();
        cart.add("A", 2);
        cart.add("F", 1);
        cart.add("A", 1);
        check("CV01 itens repetidos somam no carrinho", cart.quantityOf("A") == 3 && cart.lines().size() == 2);
        check("CV02 total do carrinho", cart.total() == 3 * 25.0 + 8.0);
        check("CV03 carrinho não baixa estoque", products.requireByCode("A").quantity() == 10);
        check("CV04 carrinho recusa mais que o estoque", throwsType(IllegalStateException.class,
                () -> cart.add("F", 5)) && cart.quantityOf("F") == 1);
        check("CV05 produto inexistente recusado", throwsType(IllegalArgumentException.class,
                () -> cart.add("X", 1)));
        check("CV06 quantidade zero recusada", throwsType(IllegalArgumentException.class,
                () -> cart.add("A", 0)));
        Cart.Line undone = cart.undoLast();
        check("CV07 desfazer remove a última inclusão (pilha)", undone.productCode().equals("A")
                && undone.quantity() == 1 && cart.quantityOf("A") == 2);
        cart.undoLast();
        check("CV08 desfazer de novo segue a ordem inversa", cart.quantityOf("F") == 0 && cart.quantityOf("A") == 2);
        cart.add("F", 2);
        check("CV09 remover produto do carrinho", cart.remove("F") && cart.quantityOf("F") == 0
                && !cart.remove("F"));
        cart.undoLast();
        check("CV10 desfazer depois de remover ignora o produto removido", cart.isEmpty());
        check("CV11 desfazer com carrinho vazio", cart.undoLast() == null);

        // Finalização
        check("CV12 venda vazia recusada", throwsType(IllegalArgumentException.class,
                () -> sales.finalizeSale(sales.newCart(), "Caixa", null, null, now)));
        Cart c1 = sales.newCart();
        c1.add("A", 3);
        c1.add("F", 2);
        Sale s1 = sales.finalizeSale(c1, "Marta", "1", "S004", now);
        check("CV13 venda grava itens, total e senha", s1.id() == 1 && s1.items().size() == 2
                && s1.total() == 3 * 25.0 + 2 * 8.0 && "S004".equals(s1.customerTicket()));
        check("CV14 venda baixa o estoque", products.requireByCode("A").quantity() == 7
                && products.requireByCode("F").quantity() == 3);
        List<StockMovement> history = inventory.historyOf("A");
        check("CV15 venda registra saída no histórico", history.get(0).type() == MovementType.EXIT
                && history.get(0).quantity() == 3 && history.get(0).responsible().equals("Marta"));

        // Estoque mudou entre montar o carrinho e finalizar: venda inteira é recusada
        Cart c2 = sales.newCart();
        c2.add("A", 1);
        c2.add("F", 3);
        inventory.registerExit("F", 2, "t");
        check("CV16 finalização sem estoque é recusada", throwsType(IllegalStateException.class,
                () -> sales.finalizeSale(c2, "Marta", null, null, now)));
        check("CV17 venda recusada não altera estoque nem grava venda",
                products.requireByCode("A").quantity() == 7 && sales.listAll().size() == 1);

        // Preço do cupom não muda com edição do produto
        products.edit("A", "Arroz 5kg", grocery, 30.0, 2);
        check("CV18 cupom mantém o preço da venda", sales.findById(1).items().get(0).unitPrice() == 25.0);

        // Consultas e ranking
        Cart c3 = sales.newCart();
        c3.add("F", 1);
        sales.finalizeSale(c3, "Marta", null, null, now.minusDays(1));
        check("CV19 vendas por data", sales.listByDate(now.toLocalDate()).size() == 1
                && sales.listByDate(now.minusDays(1).toLocalDate()).size() == 1);
        check("CV20 lista mais recente primeiro", sales.listAll().get(0).id() == 2);
        SaleService.SalesSummary summary = sales.summarize(sales.listAll());
        check("CV21 resumo de vendas", summary.sales() == 2 && summary.units() == 6
                && summary.revenue() == 91.0 + 8.0 && summary.averageTicket() == 49.5);
        List<TopSellingProduct> top = inventory.topSelling(5);
        check("CV22 mais vendidos usa as vendas reais", top.size() == 2
                && top.get(0).product().code().equals("A") && top.get(0).unitsSold() == 3
                && top.get(1).unitsSold() == 3);
        check("CV23 venda inexistente", sales.findById(99) == null);

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        return failed;
    }

    public static void main(String[] args) {
        System.exit(run() == 0 ? 0 : 1);
    }
}
