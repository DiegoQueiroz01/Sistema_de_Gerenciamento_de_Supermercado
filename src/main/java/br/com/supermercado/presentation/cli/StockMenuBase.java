package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.util.ArrayList;
import java.util.List;

// Classe mãe dos menus de produtos e estoque (StockistMenu e ProductAdminMenu).
// Aqui ficam as opções que os dois menus têm em comum.
abstract class StockMenuBase {
    protected final InputReader in;
    protected final TerminalOutput out;
    protected final ProductService products;
    protected final InventoryService inventory;
    protected final String responsible;
    private final ProductSelector selector;

    protected StockMenuBase(InputReader in, TerminalOutput out, ProductService products,
                            InventoryService inventory, String responsible) {
        this.in = in;
        this.out = out;
        this.products = products;
        this.inventory = inventory;
        this.responsible = responsible;
        this.selector = new ProductSelector(in, out, products);
    }

    private void show(String text) {
        out.blank();
        out.println(text);
    }

    // ---------- consultas ----------

    protected void listAll() {
        show(productsTable(products.listAll()));
    }

    protected void search() {
        out.title("Pesquisar Produto");
        String term = in.readRequired("Código ou parte do nome: ");
        List<Product> result;
        Product byCode = products.findByCode(term);
        if (byCode != null) {
            result = new ArrayList<>();
            result.add(byCode);
        } else {
            result = products.searchByName(term);
        }
        show(productsTable(result));
    }

    protected void filterByCategory() {
        out.title("Filtrar por Categoria");
        Category category = chooseCategory();
        if (category != null) {
            show(productsTable(products.filterByCategory(category)));
        }
    }

    protected void listSorted() {
        out.title("Listar Produtos Ordenados");
        out.println("  1) Nome   2) Preço   3) Quantidade");
        int criterion = in.readInt("Critério: ", 1, 3);
        int order = in.readInt("Ordem (1 = crescente, 2 = decrescente): ", 1, 2);
        ProductService.SortCriterion sort;
        if (criterion == 1) {
            sort = ProductService.SortCriterion.NAME;
        } else if (criterion == 2) {
            sort = ProductService.SortCriterion.PRICE;
        } else {
            sort = ProductService.SortCriterion.QUANTITY;
        }
        show(productsTable(products.listSorted(sort, order == 2)));
    }

    protected void belowMinimum() {
        out.title("Alertas de Estoque");
        show(alertsTable(inventory.listBelowMinimum()));
    }

    protected void withoutMovement() {
        out.title("Produtos Sem Movimentação");
        int days = in.readInt("Período em dias (sugestão: 30) [1-3650]: ", 1, 3650);
        show(withoutMovementTable(inventory.listWithoutMovement(days)));
    }

    protected void topSelling() {
        out.title("Produtos Mais Vendidos");
        int n = in.readInt("Quantos produtos listar? [1-100]: ", 1, 100);
        List<TopSellingProduct> ranking = inventory.topSelling(n);
        if (ranking.isEmpty()) {
            show("  Ainda não há vendas registradas.");
            return;
        }
        String text = "";
        for (int i = 0; i < ranking.size(); i++) {
            TopSellingProduct t = ranking.get(i);
            text += String.format("  %2d. %-40s %6d un%n", i + 1, t.product().name(), t.unitsSold());
        }
        show(text.stripTrailing());
    }

    protected void history() {
        out.title("Histórico de Movimentações");
        out.println("  1) De um produto específico   2) Geral (todos os produtos)");
        if (in.readInt("Escolha: ", 1, 2) == 1) {
            Product p = selectProduct();
            if (p != null) {
                out.println("  Produto: " + p.name() + " | Saldo: " + p.quantity() + " un");
                show(movementsTable(inventory.historyOf(p.code()), false));
            }
        } else {
            show(movementsTable(inventory.fullHistory(), true));
        }
    }

    // Mostra as categorias numeradas. Devolve null se cancelar.
    protected Category chooseCategory() {
        List<Category> categories = products.listCategories();
        if (categories.isEmpty()) {
            out.error("Não há categorias cadastradas.");
            return null;
        }
        for (int i = 0; i < categories.size(); i++) {
            out.println("  " + (i + 1) + ") " + categories.get(i).name());
        }
        out.println("  0) Cancelar");
        int n = in.readInt("Categoria: ", 0, categories.size());
        if (n == 0) {
            return null;
        }
        return categories.get(n - 1);
    }

    protected Product selectProduct() {
        return selector.select("Código ou nome do produto: ");
    }

    // ---------- entradas e saídas ----------

    protected void registerEntry() {
        out.title("Registrar Entrada de Estoque");
        Product p = selectProduct();
        if (p == null) {
            return;
        }
        out.println("  Saldo atual de '" + p.name() + "': " + p.quantity() + " un");
        int quantity = in.readInt("Quantidade recebida (1 a 1000000): ", 1, 1_000_000);
        out.println("  Novo saldo previsto: " + (p.quantity() + quantity) + " un");
        if (!in.confirm("Confirmar entrada?")) {
            out.warning("Operação cancelada.");
            return;
        }
        inventory.registerEntry(p.code(), quantity, responsible);
        out.success("Entrada registrada. Saldo atual: " + p.quantity() + " un");
        warnIfBelowMinimum(p);
    }

    // Saída manual: perda, avaria, vencimento ou consumo interno. Vendas são feitas no caixa.
    protected void registerExit() {
        out.title("Registrar Saída de Estoque");
        out.println("  Use para perdas, avarias, vencimentos e consumo interno. Vendas são registradas no caixa.");
        Product p = selectProduct();
        if (p == null) {
            return;
        }
        if (p.quantity() == 0) {
            out.error("'" + p.name() + "' está sem estoque.");
            return;
        }
        out.println("  Saldo atual de '" + p.name() + "': " + p.quantity() + " un");
        int quantity = in.readInt("Quantidade que sai (1 a " + p.quantity() + "): ", 1, p.quantity());
        out.println("  Novo saldo previsto: " + (p.quantity() - quantity) + " un");
        if (!in.confirm("Confirmar saída?")) {
            out.warning("Operação cancelada.");
            return;
        }
        inventory.registerExit(p.code(), quantity, responsible);
        out.success("Saída registrada. Saldo atual: " + p.quantity() + " un");
        warnIfBelowMinimum(p);
    }

    protected void warnIfBelowMinimum(Product p) {
        if (p.isBelowMinimum()) {
            out.warning("ALERTA: '" + p.name() + "' está abaixo do estoque mínimo ("
                    + p.quantity() + "/" + p.minimumStock() + ").");
        }
    }

    // ---------- tabelas ----------

    private String productsTable(List<Product> list) {
        if (list.isEmpty()) {
            return "  (nenhum produto encontrado)";
        }
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %12s %6s %6s  %s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "PREÇO", "QTD", "MÍN", "SITUAÇÃO"));
        sb.append("  ").append("-".repeat(98)).append('\n');
        for (Product p : list) {
            String situation = p.isBelowMinimum() ? "ABAIXO DO MÍNIMO" : "ok";
            sb.append(String.format(format, p.code(), TerminalOutput.cut(p.name(), 36),
                    TerminalOutput.cut(p.category().name(), 12), TerminalOutput.money(p.price()),
                    p.quantity(), p.minimumStock(), situation));
        }
        sb.append("  Total: ").append(list.size()).append(" produto(s)");
        return sb.toString();
    }

    private String alertsTable(List<Product> list) {
        if (list.isEmpty()) {
            return "  Nenhum produto abaixo do estoque mínimo.";
        }
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %6s %6s %8s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "DISP.", "MÍN", "DÉFICIT"));
        sb.append("  ").append("-".repeat(84)).append('\n');
        for (Product p : list) {
            sb.append(String.format(format, p.code(), TerminalOutput.cut(p.name(), 36),
                    TerminalOutput.cut(p.category().name(), 12), p.quantity(), p.minimumStock(),
                    p.quantity() - p.minimumStock()));
        }
        sb.append("  Estoque crítico: ").append(list.size()).append(" item(ns)");
        return sb.toString();
    }

    private String withoutMovementTable(List<Product> list) {
        if (list.isEmpty()) {
            return "  Nenhum produto parado no período.";
        }
        StringBuilder sb = new StringBuilder();
        String format = "  %-10s %-36s %-12s %6s  %s%n";
        sb.append(String.format(format, "CÓDIGO", "NOME", "CATEGORIA", "QTD", "INATIVO HÁ"));
        sb.append("  ").append("-".repeat(80)).append('\n');
        for (Product p : list) {
            sb.append(String.format(format, p.code(), TerminalOutput.cut(p.name(), 36),
                    TerminalOutput.cut(p.category().name(), 12), p.quantity(),
                    inventory.daysWithoutMovement(p) + " dias"));
        }
        return sb.toString().stripTrailing();
    }

    private String movementsTable(List<StockMovement> list, boolean showProduct) {
        if (list.isEmpty()) {
            return "  (nenhuma movimentação registrada)";
        }
        StringBuilder sb = new StringBuilder();
        String format = "  %-16s %-8s %6s  %-34s %s%n";
        sb.append(String.format(format, "DATA", "TIPO", "QTD", showProduct ? "PRODUTO" : "", "RESPONSÁVEL"));
        sb.append("  ").append("-".repeat(90)).append('\n');
        for (StockMovement m : list) {
            String qty;
            String typeLabel;
            if (m.type() == MovementType.ENTRY) {
                qty = "+" + m.quantity();
                typeLabel = "ENTRADA";
            } else {
                qty = "-" + m.quantity();
                typeLabel = "SAÍDA";
            }
            String productName = "";
            if (showProduct) {
                // produto desativado não é mais encontrado: mostra só o código
                Product p = products.findByCode(m.productCode());
                productName = p != null ? p.name() : m.productCode();
                productName = TerminalOutput.cut(productName, 34);
            }
            sb.append(String.format(format, TerminalOutput.dateTime(m.timestamp()), typeLabel, qty,
                    productName, m.responsible()));
        }
        return sb.toString().stripTrailing();
    }
}
