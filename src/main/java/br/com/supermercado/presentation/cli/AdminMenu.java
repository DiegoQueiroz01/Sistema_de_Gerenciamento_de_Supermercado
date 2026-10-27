package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.domain.user.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Menu do ADMINISTRADOR: produtos e estoque, caixas e filas, vendas, usuários e a própria senha.
 */
public final class AdminMenu {
    private final AppServices app;
    private final InputReader in;
    private final TerminalOutput out;
    private final User user;

    public AdminMenu(AppServices app, InputReader in, TerminalOutput out, User user) {
        this.app = app;
        this.in = in;
        this.out = out;
        this.user = user;
    }

    public void run() {
        int option;
        do {
            option = in.readMenuWithZeroLabel("MENU DO ADMINISTRADOR - " + user.name(), "Sair (logout)",
                    "Produtos, categorias e estoque", "Caixas e filas", "Vendas",
                    "Usuários", "Alterar minha senha");
            switch (option) {
                case 1 -> new ProductAdminMenu(in, out, app.products(), app.inventory(), user.name()).run();
                case 2 -> new QueueMenu(in, out, app.queues(), app.dashboard()).run();
                case 3 -> salesMenu();
                case 4 -> new UserAdminMenu(in, out, app.auth(), user).run();
                case 5 -> UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
                default -> { }
            }
        } while (option != 0);
    }

    private void salesMenu() {
        int option;
        do {
            option = in.readMenuOption("VENDAS", "Vendas de hoje", "Vendas de uma data",
                    "Todas as vendas", "Consultar venda pelo número");
            try {
                switch (option) {
                    case 1 -> showSales("Vendas de Hoje", app.sales().listByDate(LocalDate.now()));
                    case 2 -> {
                        LocalDate date = in.readDate("Data (dd/mm/aaaa, vazio = hoje): ", LocalDate.now());
                        showSales("Vendas de " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                app.sales().listByDate(date));
                    }
                    case 3 -> showSales("Todas as Vendas", app.sales().listAll());
                    case 4 -> {
                        long id = in.readInt("Número da venda: ", 1, Integer.MAX_VALUE);
                        Sale sale = app.sales().findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada: " + id));
                        out.blank();
                        out.println(SaleViews.receipt(sale, null));
                    }
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void showSales(String title, List<Sale> sales) {
        out.title(title);
        out.println(SaleViews.table(sales, app.sales().summarize(sales)));
    }
}
