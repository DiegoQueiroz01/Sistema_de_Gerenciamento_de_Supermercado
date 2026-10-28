package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.time.LocalDateTime;

// Parte do menu do ADMINISTRADOR: produtos, categorias, estoque e relatórios
public class ProductAdminMenu extends StockMenuBase {

    public ProductAdminMenu(InputReader in, TerminalOutput out, ProductService products,
                            InventoryService inventory, String responsible) {
        super(in, out, products, inventory, responsible);
    }

    public void run() {
        int option;
        do {
            option = in.readMenuOption("PRODUTOS E ESTOQUE (ADMINISTRADOR)",
                    "Produtos (cadastro, edição e consultas)", "Estoque e relatórios", "Categorias");
            if (option == 1) {
                productsMenu();
            } else if (option == 2) {
                reportsMenu();
            } else if (option == 3) {
                categoriesMenu();
            }
        } while (option != 0);
    }

    private void productsMenu() {
        int option;
        do {
            option = in.readMenuOption("PRODUTOS", "Cadastrar produto", "Editar produto",
                    "Definir estoque mínimo", "Pesquisar por código ou nome", "Filtrar por categoria",
                    "Listar ordenado", "Listar todos", "Desativar produto");
            try {
                switch (option) {
                    case 1: registerProduct(); break;
                    case 2: editProduct(); break;
                    case 3: defineMinimumStock(); break;
                    case 4: search(); break;
                    case 5: filterByCategory(); break;
                    case 6: listSorted(); break;
                    case 7: listAll(); break;
                    case 8: deactivateProduct(); break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void reportsMenu() {
        int option;
        do {
            option = in.readMenuOption("ESTOQUE E RELATÓRIOS", "Registrar entrada de estoque",
                    "Registrar saída (perda, avaria, consumo)", "Produtos abaixo do estoque mínimo",
                    "Produtos sem movimentação", "Produtos mais vendidos", "Histórico de movimentações");
            try {
                switch (option) {
                    case 1: registerEntry(); break;
                    case 2: registerExit(); break;
                    case 3: belowMinimum(); break;
                    case 4: withoutMovement(); break;
                    case 5: topSelling(); break;
                    case 6: history(); break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void categoriesMenu() {
        int option;
        do {
            option = in.readMenuOption("CATEGORIAS", "Listar categorias", "Nova categoria");
            try {
                if (option == 1) {
                    out.title("Categorias");
                    for (Category c : products.listCategories()) {
                        out.println("  - " + c.name());
                    }
                } else if (option == 2) {
                    products.registerCategory(in.readRequired("Nome da categoria: "));
                    out.success("Categoria cadastrada.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void registerProduct() {
        out.title("Novo Produto");
        if (products.listCategories().isEmpty()) {
            out.error("Cadastre ao menos uma categoria antes de cadastrar produtos.");
            return;
        }
        String code = in.readRequired("Código de barras: ");
        while (products.findByCode(code) != null) {
            out.error("Código já cadastrado no sistema.");
            code = in.readRequired("Código de barras: ");
        }
        String name = in.readRequired("Nome do produto: ");
        Category category = chooseCategory();
        if (category == null) {
            return;
        }
        double price = in.readPositiveDecimal("Preço de venda (R$): ");
        int quantity = in.readInt("Quantidade disponível (0 a 1000000): ", 0, 1_000_000);
        int minimum = in.readInt("Estoque mínimo (0 a 1000000): ", 0, 1_000_000);
        LocalDateTime now = LocalDateTime.now();
        Product p = products.registerProduct(code, name, category, price, minimum, now);
        if (quantity > 0) {
            inventory.registerEntry(p.code(), quantity, responsible, now); // estoque inicial entra como ENTRADA
        }
        out.success("Produto cadastrado: " + p.name());
        warnIfBelowMinimum(p);
    }

    private void editProduct() {
        out.title("Editar Produto");
        Product p = selectProduct();
        if (p == null) {
            return;
        }
        out.println("Deixe em branco para manter o valor atual.");
        String name = in.readLine("Nome [" + p.name() + "]: ");
        if (name.isEmpty()) {
            name = p.name();
        }
        Category category = p.category();
        if (in.confirm("Alterar a categoria (atual: " + category.name() + ")?")) {
            Category chosen = chooseCategory();
            if (chosen != null) {
                category = chosen;
            }
        }
        String priceText = in.readLine("Preço [" + TerminalOutput.money(p.price()) + "]: ").replace(',', '.');
        double price = p.price();
        if (!priceText.isEmpty()) {
            price = parseDecimal(priceText);
        }
        String minText = in.readLine("Estoque mínimo [" + p.minimumStock() + "]: ");
        int minimum = p.minimumStock();
        if (!minText.isEmpty()) {
            minimum = parseInt(minText);
        }
        products.edit(p.code(), name, category, price, minimum);
        out.success("Produto atualizado. (A quantidade só muda por movimentações.)");
        warnIfBelowMinimum(p);
    }

    private void deactivateProduct() {
        out.title("Desativar Produto");
        Product p = selectProduct();
        if (p == null) {
            return;
        }
        out.println("  O produto sai das consultas e das vendas; o histórico é mantido.");
        out.println("  Para reativar, cadastre de novo o mesmo código.");
        if (!in.confirm("Desativar '" + p.name() + "'?")) {
            out.warning("Operação cancelada.");
            return;
        }
        products.deactivate(p.code());
        out.success("Produto desativado: " + p.name());
    }

    private void defineMinimumStock() {
        out.title("Definir Estoque Mínimo");
        Product p = selectProduct();
        if (p == null) {
            return;
        }
        int minimum = in.readInt("Novo estoque mínimo para '" + p.name() + "' (atual "
                + p.minimumStock() + "): ", 0, 1_000_000);
        products.defineMinimumStock(p.code(), minimum);
        out.success("Estoque mínimo atualizado para " + minimum + ".");
        warnIfBelowMinimum(p);
    }

    private double parseDecimal(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Preço inválido: " + text);
        }
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: " + text);
        }
    }
}
