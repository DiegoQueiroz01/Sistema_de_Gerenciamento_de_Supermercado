package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;


/**
 * Menu do ESTOQUISTA: consultar produtos, registrar entradas e saídas, alertas, itens parados e histórico.
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
                    "Registrar entrada de estoque", "Registrar saída (perda, avaria, consumo)",
                    "Alertas: abaixo do estoque mínimo",
                    "Produtos sem movimentação", "Histórico de movimentações");
            try {
                switch (option) {
                    case 1 -> listAll();
                    case 2 -> search();
                    case 3 -> filterByCategory();
                    case 4 -> listSorted();
                    case 5 -> registerEntry();
                    case 6 -> registerExit();
                    case 7 -> belowMinimum();
                    case 8 -> withoutMovement();
                    case 9 -> history();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }
}
