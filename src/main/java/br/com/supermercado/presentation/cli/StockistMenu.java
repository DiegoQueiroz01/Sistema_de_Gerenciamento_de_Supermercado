package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;

// Menu do ESTOQUISTA: consultar produtos, entradas, saídas, alertas e histórico.
// "responsible" é o nome de quem fez login (vai no histórico das movimentações).
public class StockistMenu extends StockMenuBase {

    public StockistMenu(InputReader in, TerminalOutput out, ProductService products,
                        InventoryService inventory, String responsible) {
        super(in, out, products, inventory, responsible);
    }

    public void run() {
        int option;
        do {
            option = in.readMenuOption("MENU DO ESTOQUISTA", "Listar todos os produtos",
                    "Pesquisar por código ou nome", "Filtrar por categoria", "Listar ordenado",
                    "Registrar entrada de estoque", "Registrar saída (perda, avaria, consumo)",
                    "Alertas: abaixo do estoque mínimo",
                    "Produtos sem movimentação", "Histórico de movimentações");
            try {
                switch (option) {
                    case 1: listAll(); break;
                    case 2: search(); break;
                    case 3: filterByCategory(); break;
                    case 4: listSorted(); break;
                    case 5: registerEntry(); break;
                    case 6: registerExit(); break;
                    case 7: belowMinimum(); break;
                    case 8: withoutMovement(); break;
                    case 9: history(); break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }
}
