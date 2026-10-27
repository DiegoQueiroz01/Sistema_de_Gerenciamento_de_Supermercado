package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Base dos menus de produtos e estoque (StockistMenu e ProductAdminMenu): consultas de
 * produtos/estoque e tabelas de visualização no terminal. A leitura e a escrita são
 * feitas por InputReader e TerminalOutput.
 */
abstract class StockMenuBase {
    private final InputReader in;
    private final TerminalOutput out;
    private final ProductSelector selector;
    protected final ProductService products;
    protected final InventoryService inventory;
    protected final String responsible;

    protected StockMenuBase(InputReader in, TerminalOutput out, ProductService products,
                            InventoryService inventory, String responsible) {
        this.in = in;
        this.out = out;
        this.products = products;
        this.inventory = inventory;
        this.responsible = responsible;
        this.selector = new ProductSelector(in, out, products);
    }

    // ================================================================ saída
    protected void println(String text) { out.println(text); }

    protected void error(String text) { out.error(text); }

    protected void success(String text) { out.success(text); }

    protected void warning(String text) { out.warning(text); }

    protected void title(String title) { out.title(title); }

    private void show(String text) {
        out.blank();
        out.println(text);
    }

    // ================================================================ entrada validada
    protected String readLine(String prompt) { return in.readLine(prompt); }

    protected String readRequired(String prompt) { return in.readRequired(prompt); }

    protected int readInt(String prompt, int min, int max) { return in.readInt(prompt, min, max); }

    protected double readPositiveDecimal(String prompt) { return in.readPositiveDecimal(prompt); }

    protected boolean confirm(String prompt) { return in.confirm(prompt); }

    protected int readMenuOption(String title, String... options) {
        return in.readMenuOption(title, options);
    }

    // ================================================================ consultas (comuns ao estoquista e ao administrador)
    protected void listAll() { show(productsTable(products.listAll())); }

    protected void search() {
        title("Pesquisar Produto");
        String term = readRequired("Código ou parte do nome: ");
        Optional<Product> byCode = products.findByCode(term);
        List<Product> result = byCode.isPresent() ? List.of(byCode.get()) : products.searchByName(term);
        show(productsTable(result));
    }

    protected void filterByCategory() {
        title("Filtrar por Categoria");
        Category category = chooseCategory();
        if (category != null) show(productsTable(products.filterByCategory(category)));
    }

    protected void listSorted() {
        title("Listar Produtos Ordenados");
        println("  1) Nome   2) Preço   3) Quantidade");
        int criterion = readInt("Critério: ", 1, 3);
        int order = readInt("Ordem (1 = crescente, 2 = decrescente): ", 1, 2);
        show(productsTable(products.listSorted(ProductService.SortCriterion.values()[criterion - 1], order == 2)));
    }

    protected void belowMinimum() {
        title("Alertas de Estoque");
        show(alertsTable(inventory.listBelowMinimum()));
    }

    protected void withoutMovement() {
        title("Produtos Sem Movimentação");
        int days = readInt("Período em dias (sugestão: 30) [1-3650]: ", 1, 3650);
        show(withoutMovementTable(inventory.listWithoutMovement(days)));
    }

    protected void topSelling() {
        title("Produtos Mais Vendidos");
        int n = readInt("Quantos produtos listar? [1-100]: ", 1, 100);
        List<TopSellingProduct> ranking = inventory.topSelling(n);
        if (ranking.isEmpty()) {
            show("  Ainda não há vendas registradas.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int position = 1;
        for (TopSellingProduct t : ranking) {
            sb.append(String.format("  %2d. %-40s %6d un%n", position++, t.product().name(), t.unitsSold()));
        }
        show(sb.toString().stripTrailing());
    }

    protected void history() {
        title("Histórico de Movimentações");
        println("  1) De um produto específico   2) Geral (todos os produtos)");
        if (readInt("Escolha: ", 1, 2) == 1) {
            Product p = selectProduct();
            if (p != null) {
                println("  Produto: " + p.name() + " | Saldo: " + p.quantity() + " un");
                show(movementsTable(inventory.historyOf(p.code()), false));
            }
        } else {
            show(movementsTable(inventory.fullHistory(), true));
        }
    }

    protected Category chooseCategory() {
        List<Category> categories = products.listCategories();
        if (categories.isEmpty()) {
            error("Não há categorias cadastradas.");
            return null;
        }
        for (int i = 0; i < categories.size(); i++) println("  " + (i + 1) + ") " + categories.get(i).name());
        println("  0) Cancelar");
        int n = readInt("Categoria: ", 0, categories.size());
        return n == 0 ? null : categories.get(n - 1);
    }

    /** Seleciona por código exato ou por parte do nome (lista numerada se houver vários). */
    protected Product selectProduct() {
        return selector.select("Código ou nome do produto: ");
    }

    protected void warnIfBelowMinimum(Product p) {
        if (p.isBelowMinimum()) {
            warning("ALERTA: '" + p.name() + "' está abaixo do estoque mínimo ("
                    + p.quantity() + "/" + p.minimumStock() + ").");
        }
    }

    // ================================================================ tabelas
    protected static String money(double value) { return TerminalOutput.money(value); }

    private static String cut(String text, int max) { return TerminalOutput.cut(text, max); }

    private static String productsTable(List<Product> list) {
        if (list.isEmpty()) return "  (nenhum produto encontrado)";
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %12s %6s %6s  %s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "PREÇO", "QTD", "MÍN", "SITUAÇÃO"));
        sb.append("  ").append("-".repeat(98)).append('\n');
        for (Product p : list) {
            sb.append(String.format(format, p.code(), cut(p.name(), 36), cut(p.category().name(), 12),
                    money(p.price()), p.quantity(), p.minimumStock(),
                    p.isBelowMinimum() ? "ABAIXO DO MÍNIMO" : "ok"));
        }
        sb.append("  Total: ").append(list.size()).append(" produto(s)");
        return sb.toString();
    }

    private static String alertsTable(List<Product> list) {
        if (list.isEmpty()) return "  Nenhum produto abaixo do estoque mínimo.";
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %6s %6s %8s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "DISP.", "MÍN", "DÉFICIT"));
        sb.append("  ").append("-".repeat(84)).append('\n');
        for (Product p : list) {
            sb.append(String.format(format, p.code(), cut(p.name(), 36), cut(p.category().name(), 12),
                    p.quantity(), p.minimumStock(), p.quantity() - p.minimumStock()));
        }
        sb.append("  Estoque crítico: ").append(list.size()).append(" item(ns)");
        return sb.toString();
    }

    private String withoutMovementTable(List<Product> list) {
        if (list.isEmpty()) return "  Nenhum produto parado no período.";
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %6s  %s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "QTD", "INATIVO HÁ"));
        sb.append("  ").append("-".repeat(80)).append('\n');
        for (Product p : list) {
            sb.append(String.format(format, p.code(), cut(p.name(), 36), cut(p.category().name(), 12),
                    p.quantity(), inventory.daysWithoutMovement(p) + " dias"));
        }
        return sb.toString().stripTrailing();
    }

    private String movementsTable(List<StockMovement> list, boolean showProduct) {
        if (list.isEmpty()) return "  (nenhuma movimentação registrada)";
        StringBuilder sb = new StringBuilder();
        String format = "  %-16s %-8s %6s  %-34s %s%n";
        sb.append(String.format(format, "DATA", "TIPO", "QTD", showProduct ? "PRODUTO" : "", "RESPONSÁVEL"));
        sb.append("  ").append("-".repeat(90)).append('\n');
        for (StockMovement m : list) {
            String qty = (m.type() == MovementType.ENTRY ? "+" : "-") + m.quantity();
            String productName = showProduct
                    ? cut(products.findByCode(m.productCode()).map(Product::name).orElse(m.productCode()), 34) : "";
            String typeLabel = m.type() == MovementType.ENTRY ? "ENTRADA" : "SAÍDA";
            sb.append(String.format(format, dateTime(m.timestamp()), typeLabel, qty, productName, m.responsible()));
        }
        return sb.toString().stripTrailing();
    }

    private static String dateTime(LocalDateTime date) { return TerminalOutput.dateTime(date); }
}
