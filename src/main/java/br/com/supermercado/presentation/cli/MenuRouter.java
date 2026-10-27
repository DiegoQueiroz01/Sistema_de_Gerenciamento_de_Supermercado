package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.domain.user.User;

/** Abre o menu correspondente ao perfil do usuário autenticado. */
public final class MenuRouter {
    private final AppServices app;
    private final InputReader in;
    private final TerminalOutput out;

    public MenuRouter(AppServices app, InputReader in, TerminalOutput out) {
        this.app = app;
        this.in = in;
        this.out = out;
    }

    /** Volta quando o usuário sai (logout). */
    public void open(User user) {
        switch (user.role()) {
            case ADMIN -> new AdminMenu(app, in, out, user).run();
            case STOCKIST -> stockistHome(user);
            case CASHIER -> cashierHome(user);
        }
    }

    private void stockistHome(User user) {
        int option;
        do {
            option = in.readMenuWithZeroLabel("ESTOQUISTA - " + user.name(), "Sair (logout)",
                    "Produtos e estoque", "Alterar minha senha");
            switch (option) {
                case 1 -> new StockistMenu(in, out, app.products(), app.inventory(), user.name()).run();
                case 2 -> UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
                default -> { }
            }
        } while (option != 0);
    }

    private void cashierHome(User user) {
        int option;
        do {
            option = in.readMenuWithZeroLabel("OPERADOR DE CAIXA - " + user.name(), "Sair (logout)",
                    "Vendas", "Caixas e filas (atendimento)", "Alterar minha senha");
            switch (option) {
                case 1 -> new CashierMenu(in, out, app.sales(), app.products(), app.queues(), app.store(),
                        user.name()).run();
                case 2 -> new QueueMenu(in, out, app.queues(), app.dashboard()).run();
                case 3 -> UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
                default -> { }
            }
        } while (option != 0);
    }
}
