package br.com.supermercado;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.application.inventory.InventoryService.SoldItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Testes do E.1 sem dependências externas.
 * Execução: java -cp target/classes:target/test-classes br.com.supermercado.E1Tests
 */
public class E1Tests {
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
        E1Demo.Setup setup = E1Demo.createSeededSetup();
        ProductService ps = setup.products();
        InventoryService inv = setup.inventory();
        Category beverages = ps.findCategory("bebidas");
        LocalDateTime now = LocalDateTime.now();

        // Cadastro
        Product juice = ps.registerProduct("999", "Suco 1L", beverages, 6.5, 5, now);
        inv.registerEntry("999", 10, "t");
        check("CT01 cadastra produto novo", juice.quantity() == 10);
        check("CT02 código duplicado recusado", throwsType(IllegalStateException.class,
                () -> ps.registerProduct("999", "Outro", beverages, 1, 1, now)));
        check("CT03 preço zero recusado", throwsType(IllegalArgumentException.class,
                () -> ps.registerProduct("998", "X", beverages, 0, 1, now)));
        check("CT04 estoque mínimo negativo recusado", throwsType(IllegalArgumentException.class,
                () -> ps.registerProduct("997", "X", beverages, 1, -1, now)));
        check("CT05 nome vazio recusado", throwsType(IllegalArgumentException.class,
                () -> ps.registerProduct("996", "  ", beverages, 1, 1, now)));
        check("CT06 categoria não cadastrada recusada", throwsType(IllegalArgumentException.class,
                () -> ps.registerProduct("995", "X", new Category("Fantasma"), 1, 1, now)));
        check("CT07 estoque inicial vira ENTRY no histórico",
                inv.historyOf("999").size() == 1 && inv.historyOf("999").get(0).type() == MovementType.ENTRY);

        // Pesquisa / filtro / ordenação
        check("CT08 busca por código", ps.findByCode("78910002") != null);
        check("CT09 busca por nome ignora acento/caixa", ps.searchByName("ype").size() == 1
                && ps.searchByName("YPÊ").size() == 1);
        check("CT10 filtro por categoria", ps.filterByCategory(ps.findCategory("Laticínios")).size() == 2);
        List<Product> byPrice = ps.listSorted(ProductService.SortCriterion.PRICE, false);
        check("CT11 ordenação por preço crescente", byPrice.get(0).price() <= byPrice.get(1).price());
        List<Product> byQty = ps.listSorted(ProductService.SortCriterion.QUANTITY, true);
        check("CT12 ordenação por quantidade decrescente", byQty.get(0).quantity() >= byQty.get(1).quantity());
        check("CT13 ordenação por nome", ps.listSorted(ProductService.SortCriterion.NAME, false).get(0).name().startsWith("Arroz"));

        // Movimentações
        Product milk = ps.requireByCode("78910002");
        int before = milk.quantity();
        inv.registerEntry("78910002", 10, "t");
        check("CT14 entrada atualiza quantidade", milk.quantity() == before + 10);
        int historySize = inv.historyOf("78910002").size();
        check("CT15 saída acima do estoque impedida", throwsType(IllegalStateException.class,
                () -> inv.registerExit("78910002", 9999, "t")));
        check("CT16 falha não altera estoque nem histórico",
                milk.quantity() == before + 10 && inv.historyOf("78910002").size() == historySize);
        check("CT17 quantidade zero recusada", throwsType(IllegalArgumentException.class,
                () -> inv.registerEntry("78910002", 0, "t")));
        check("CT18 produto inexistente recusado", throwsType(IllegalArgumentException.class,
                () -> inv.registerEntry("000", 1, "t")));
        check("CT19 histórico mais recente primeiro", inv.historyOf("78910002").get(0).type() == MovementType.ENTRY);
        inv.registerExit("78910002", 1, "t");
        check("CT20 saída válida baixa estoque", milk.quantity() == before + 9);

        // Abaixo do mínimo / sem movimentação
        List<Product> below = inv.listBelowMinimum();
        check("CT21 abaixo do mínimo detectado (Arroz)", below.contains(ps.requireByCode("78910005")));
        check("CT22 acima do mínimo não aparece (Leite)", !below.contains(milk));
        inv.registerEntry("78910005", 100, "t");
        check("CT23 reposição remove do alerta", !inv.listBelowMinimum().contains(ps.requireByCode("78910005")));
        check("CT24 sem movimentação 30 dias = 4 produtos", inv.listWithoutMovement(30).size() == 4);
        check("CT25 período inválido recusado", throwsType(IllegalArgumentException.class,
                () -> inv.listWithoutMovement(0)));

        // Integração com vendas (E.2)
        Product rice = ps.requireByCode("78910005");
        Product beans = ps.requireByCode("78910011");
        int rq = rice.quantity(), bq = beans.quantity();
        List<SoldItem> bad = List.of(new SoldItem("78910005", 1), new SoldItem("78910011", bq + 1));
        check("CT26 venda com item sem estoque recusada", throwsType(IllegalStateException.class,
                () -> inv.registerSaleExit(now, bad, "t")));
        check("CT27 venda recusada é atômica", rice.quantity() == rq && beans.quantity() == bq);
        inv.registerSaleExit(now, List.of(new SoldItem("78910005", 2), new SoldItem("78910005", 1)), "t");
        check("CT28 venda válida baixa estoque (itens repetidos somam)", rice.quantity() == rq - 3);
        check("CT29 hasStock", inv.hasStock("78910011", 1) && !inv.hasStock("78910011", 99999)
                && !inv.hasStock("000", 1));
        check("CT30 venda sem itens é inválida", throwsType(IllegalArgumentException.class,
                () -> inv.registerSaleExit(now, List.of(), "t")));
        List<TopSellingProduct> top = inv.topSelling(3);
        check("CT31 mais vendidos: Leite em 1º (30 un)", top.size() == 3
                && top.get(0).product().code().equals("78910002") && top.get(0).unitsSold() == 30);
        check("CT32 limite do ranking inválido recusado", throwsType(IllegalArgumentException.class,
                () -> inv.topSelling(0)));

        // Edição e categorias
        ps.edit("78910005", "Arroz 5kg", rice.category(), 26.0, 20);
        check("CT33 edição altera preço e mínimo", rice.price() == 26.0 && rice.minimumStock() == 20);
        check("CT34 edição inválida não altera nada", throwsType(IllegalArgumentException.class,
                () -> ps.edit("78910005", "Novo", rice.category(), -5, 20)) && rice.name().equals("Arroz 5kg"));
        ps.defineMinimumStock("78910005", 7);
        check("CT35 definir estoque mínimo", rice.minimumStock() == 7);
        check("CT36 categoria duplicada recusada", throwsType(IllegalStateException.class,
                () -> ps.registerCategory("bebidas")));
        check("CT37 categoria vazia recusada", throwsType(IllegalArgumentException.class,
                () -> ps.registerCategory("  ")));

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        return failed;
    }

    public static void main(String[] args) {
        System.exit(run() == 0 ? 0 : 1);
    }
}
