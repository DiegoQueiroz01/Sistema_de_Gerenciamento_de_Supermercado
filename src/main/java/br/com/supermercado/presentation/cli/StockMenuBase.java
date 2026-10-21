package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * Base dos menus do E.1 (StockistMenu e ProductAdminMenu): leitura validada, mensagens,
 * consultas de produtos/estoque e tabelas de visualização no terminal.
 * Usa Scanner/PrintStream diretamente para o E.1 funcionar sem depender de InputReader/TerminalOutput
 * (que são do E.2); na integração, os métodos de E/S abaixo podem delegar a essas classes.
 * Se o teclado fechar (EOF), Scanner lança NoSuchElementException.
 */
abstract class StockMenuBase {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Scanner scanner;
    private final PrintStream out;
    protected final ProductService products;
    protected final InventoryService inventory;
    protected final String responsible;

    protected StockMenuBase(Scanner scanner, PrintStream out, ProductService products,
                            InventoryService inventory, String responsible) {
        this.scanner = scanner;
        this.out = out;
        this.products = products;
        this.inventory = inventory;
        this.responsible = responsible;
    }

    // ================================================================ saída
    protected void println(String text) { out.println(text); }

    protected void error(String text) { out.println("  [ERRO] " + text); }

    protected void success(String text) { out.println("  [OK] " + text); }

    protected void warning(String text) { out.println("  [!] " + text); }

    protected void title(String title) {
        out.println();
        out.println("=== " + title + " ===");
    }

    private void show(String text) {
        out.println();
        out.println(text);
    }

    // ================================================================ entrada validada
    protected String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.nextLine().trim();
    }

    protected String readRequired(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) return text;
            error("Este campo é obrigatório.");
        }
    }

    protected int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) return value;
                error("Informe um número entre " + min + " e " + max + ".");
            } catch (NumberFormatException e) {
                error("Entrada inválida: digite um número inteiro.");
            }
        }
    }

    /** Aceita vírgula ou ponto como separador decimal; exige valor maior que zero. */
    protected double readPositiveDecimal(String prompt) {
        while (true) {
            String text = readLine(prompt).replace(',', '.');
            try {
                double value = Double.parseDouble(text);
                if (value > 0 && !Double.isInfinite(value)) return value;
                error("O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                error("Entrada inválida: digite um número (ex.: 4,20).");
            }
        }
    }

    protected boolean confirm(String prompt) {
        while (true) {
            String text = readLine(prompt + " (s/n): ").toLowerCase();
            if (text.equals("s") || text.equals("sim")) return true;
            if (text.equals("n") || text.equals("nao") || text.equals("não")) return false;
            error("Responda com 's' ou 'n'.");
        }
    }

    /** Menu numerado (0 = voltar/sair); devolve a opção escolhida. */
    protected int readMenuOption(String title, String... options) {
        title(title);
        for (int i = 0; i < options.length; i++) {
            println("  " + (i + 1) + ") " + options[i]);
        }
        println("  0) Voltar/Sair");
        return readInt("Escolha: ", 0, options.length);
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
        String term = readRequired("Código ou nome do produto: ");
        Optional<Product> exact = products.findByCode(term);
        if (exact.isPresent()) return exact.get();
        List<Product> found = new ArrayList<>(products.searchByName(term));
        if (found.isEmpty()) {
            error("Produto não encontrado.");
            return null;
        }
        if (found.size() == 1) return found.get(0);
        for (int i = 0; i < found.size(); i++) {
            Product p = found.get(i);
            println("  " + (i + 1) + ") " + p.code() + " - " + p.name() + " (" + p.quantity() + " un)");
        }
        println("  0) Cancelar");
        int n = readInt("Escolha o produto: ", 0, found.size());
        return n == 0 ? null : found.get(n - 1);
    }

    protected void warnIfBelowMinimum(Product p) {
        if (p.isBelowMinimum()) {
            warning("ALERTA: '" + p.name() + "' está abaixo do estoque mínimo ("
                    + p.quantity() + "/" + p.minimumStock() + ").");
        }
    }

    // ================================================================ tabelas
    protected static String money(double value) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", value);
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

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

    private static String dateTime(LocalDateTime date) { return date.format(DATE_TIME); }
}
