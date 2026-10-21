package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Parte do menu do ADMINISTRADOR que pertence ao E.1: produtos, categorias e relatórios de estoque.
 * O AdminMenu apenas chama {@link #run()} em uma de suas opções.
 */
public final class ProductAdminMenu extends StockMenuBase {

    public ProductAdminMenu(Scanner scanner, PrintStream out, ProductService products,
                            InventoryService inventory, String responsible) {
        super(scanner, out, products, inventory, responsible);
    }

    public void run() {
        int option;
        do {
            option = readMenuOption("PRODUTOS E ESTOQUE (ADMINISTRADOR)",
                    "Produtos (cadastro, edição e consultas)", "Estoque e relatórios", "Categorias");
            switch (option) {
                case 1 -> productsMenu();
                case 2 -> reportsMenu();
                case 3 -> categoriesMenu();
                default -> { }
            }
        } while (option != 0);
    }

    private void productsMenu() {
        int option;
        do {
            option = readMenuOption("PRODUTOS", "Cadastrar produto", "Editar produto",
                    "Definir estoque mínimo", "Pesquisar por código ou nome", "Filtrar por categoria",
                    "Listar ordenado", "Listar todos");
            try {
                switch (option) {
                    case 1 -> registerProduct();
                    case 2 -> editProduct();
                    case 3 -> defineMinimumStock();
                    case 4 -> search();
                    case 5 -> filterByCategory();
                    case 6 -> listSorted();
                    case 7 -> listAll();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }

    private void reportsMenu() {
        int option;
        do {
            option = readMenuOption("ESTOQUE E RELATÓRIOS", "Produtos abaixo do estoque mínimo",
                    "Produtos sem movimentação", "Produtos mais vendidos", "Histórico de movimentações");
            try {
                switch (option) {
                    case 1 -> belowMinimum();
                    case 2 -> withoutMovement();
                    case 3 -> topSelling();
                    case 4 -> history();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }

    private void categoriesMenu() {
        int option;
        do {
            option = readMenuOption("CATEGORIAS", "Listar categorias", "Nova categoria");
            try {
                if (option == 1) {
                    title("Categorias");
                    products.listCategories().forEach(c -> println("  - " + c.name()));
                } else if (option == 2) {
                    products.registerCategory(readRequired("Nome da categoria: "));
                    success("Categoria cadastrada.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }

    private void registerProduct() {
        title("Novo Produto");
        if (products.listCategories().isEmpty()) {
            error("Cadastre ao menos uma categoria antes de cadastrar produtos.");
            return;
        }
        String code;
        while (true) {
            code = readRequired("Código de barras: ");
            if (products.findByCode(code).isEmpty()) break;
            error("Código já cadastrado no sistema.");
        }
        String name = readRequired("Nome do produto: ");
        Category category = chooseCategory();
        if (category == null) return;
        double price = readPositiveDecimal("Preço de venda (R$): ");
        int quantity = readInt("Quantidade disponível (0 a 1000000): ", 0, 1_000_000);
        int minimum = readInt("Estoque mínimo (0 a 1000000): ", 0, 1_000_000);
        LocalDateTime now = LocalDateTime.now();
        Product p = products.registerProduct(code, name, category, price, minimum, now);
        if (quantity > 0) {
            inventory.registerEntry(p.code(), quantity, responsible, now); // estoque inicial = ENTRY
        }
        success("Produto cadastrado: " + p.name());
        // Relê o produto: com MySQL cada consulta devolve uma cópia nova.
        warnIfBelowMinimum(products.requireByCode(p.code()));
    }

    private void editProduct() {
        title("Editar Produto");
        Product p = selectProduct();
        if (p == null) return;
        println("Deixe em branco para manter o valor atual.");
        String name = readLine("Nome [" + p.name() + "]: ");
        if (name.isEmpty()) name = p.name();
        Category category = p.category();
        if (confirm("Alterar a categoria (atual: " + category.name() + ")?")) {
            Category chosen = chooseCategory();
            if (chosen != null) category = chosen;
        }
        String priceText = readLine("Preço [" + money(p.price()) + "]: ").replace(',', '.');
        double price = priceText.isEmpty() ? p.price() : parseDecimal(priceText);
        String minText = readLine("Estoque mínimo [" + p.minimumStock() + "]: ");
        int minimum = minText.isEmpty() ? p.minimumStock() : parseInt(minText);
        products.edit(p.code(), name, category, price, minimum);
        success("Produto atualizado. (A quantidade só muda por movimentações.)");
        warnIfBelowMinimum(products.requireByCode(p.code()));
    }

    private void defineMinimumStock() {
        title("Definir Estoque Mínimo");
        Product p = selectProduct();
        if (p == null) return;
        int minimum = readInt("Novo estoque mínimo para '" + p.name() + "' (atual "
                + p.minimumStock() + "): ", 0, 1_000_000);
        products.defineMinimumStock(p.code(), minimum);
        success("Estoque mínimo atualizado para " + minimum + ".");
        warnIfBelowMinimum(products.requireByCode(p.code()));
    }

    private static double parseDecimal(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Preço inválido: " + text);
        }
    }

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: " + text);
        }
    }
}
