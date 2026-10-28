package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.domain.user.User;
import br.com.supermercado.domain.user.UserRole;

// Abre o menu certo de acordo com o perfil do usuário que fez login
public class MenuRouter {
    private final AppServices app;
    private final InputReader in;
    private final TerminalOutput out;

    public MenuRouter(AppServices app, InputReader in, TerminalOutput out) {
        this.app = app;
        this.in = in;
        this.out = out;
    }

    // Só volta quando o usuário escolhe sair (logout)
    public void open(User user) {
        if (user.role() == UserRole.ADMIN) {
            new AdminMenu(app, in, out, user).run();
        } else if (user.role() == UserRole.STOCKIST) {
            stockistHome(user);
        } else if (user.role() == UserRole.CASHIER) {
            cashierHome(user);
        }
    }

    private void stockistHome(User user) {
        int option;
        do {
            option = in.readMenuWithZeroLabel("ESTOQUISTA - " + user.name(), "Sair (logout)",
                    "Produtos e estoque", "Alterar minha senha");
            if (option == 1) {
                new StockistMenu(in, out, app.products(), app.inventory(), user.name()).run();
            } else if (option == 2) {
                UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
            }
        } while (option != 0);
    }

    private void cashierHome(User user) {
        int option;
        do {
            option = in.readMenuWithZeroLabel("OPERADOR DE CAIXA - " + user.name(), "Sair (logout)",
                    "Vendas", "Caixas e filas (atendimento)", "Alterar minha senha");
            if (option == 1) {
                new CashierMenu(in, out, app.sales(), app.products(), app.queues(), app.store(), user.name()).run();
            } else if (option == 2) {
                new QueueMenu(in, out, app.queues(), app.dashboard()).run();
            } else if (option == 3) {
                UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
            }
        } while (option != 0);
    }
}
