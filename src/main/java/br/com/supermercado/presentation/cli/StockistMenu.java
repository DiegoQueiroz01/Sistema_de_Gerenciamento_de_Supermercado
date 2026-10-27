package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Product;


/**
 * Menu do ESTOQUISTA (E.1): consultar produtos, registrar entradas, alertas, itens parados e histórico.
 * O MenuRouter informa o nome do usuário autenticado em {@code responsible}.
 */
public final class StockistMenu extends StockMenuBase {

    public StockistMenu(InputReader in, TerminalOutput out, ProductService products,
                        InventoryService inventory, String responsible) {
        super(in, out, products, inventory, responsible);
    }

    public void run() {
        int option;
        do {
            option = readMenuOption("MENU DO ESTOQUISTA", "Listar todos os produtos",
                    "Pesquisar por código ou nome", "Filtrar por categoria", "Listar ordenado",
                    "Registrar entrada de estoque", "Alertas: abaixo do estoque mínimo",
                    "Produtos sem movimentação", "Histórico de movimentações");
            try {
                switch (option) {
                    case 1 -> listAll();
                    case 2 -> search();
                    case 3 -> filterByCategory();
                    case 4 -> listSorted();
                    case 5 -> registerEntry();
                    case 6 -> belowMinimum();
                    case 7 -> withoutMovement();
                    case 8 -> history();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }

    private void registerEntry() {
        title("Registrar Entrada de Estoque");
        Product p = selectProduct();
        if (p == null) return;
        println("  Saldo atual de '" + p.name() + "': " + p.quantity() + " un");
        int quantity = readInt("Quantidade recebida (1 a 1000000): ", 1, 1_000_000);
        println("  Novo saldo previsto: " + (p.quantity() + quantity) + " un");
        if (!confirm("Confirmar entrada?")) {
            warning("Operação cancelada.");
            return;
        }
        inventory.registerEntry(p.code(), quantity, responsible);
        // Relê o produto: com MySQL cada consulta devolve uma cópia nova.
        Product updated = products.requireByCode(p.code());
        success("Entrada registrada. Saldo atual: " + updated.quantity() + " un");
        warnIfBelowMinimum(updated);
    }
}
