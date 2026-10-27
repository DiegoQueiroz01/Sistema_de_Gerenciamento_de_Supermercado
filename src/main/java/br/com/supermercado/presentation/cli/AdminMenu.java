package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.application.inventory.InventoryService.TopSellingProduct;
import br.com.supermercado.application.reporting.DashboardService.QueueOverview;
import br.com.supermercado.application.reporting.DashboardService.StoreOverview;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.domain.user.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Menu do ADMINISTRADOR: visão geral, produtos e estoque, caixas e filas, vendas, usuários,
 * dados do supermercado, parâmetros e a própria senha.
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
                    "Visão geral", "Produtos, categorias e estoque", "Caixas e filas", "Vendas",
                    "Usuários", "Dados do supermercado", "Parâmetros", "Alterar minha senha");
            try {
                switch (option) {
                    case 1 -> overview();
                    case 2 -> new ProductAdminMenu(in, out, app.products(), app.inventory(), user.name()).run();
                    case 3 -> new QueueMenu(in, out, app.queues(), app.dashboard()).run();
                    case 4 -> salesMenu();
                    case 5 -> new UserAdminMenu(in, out, app.auth(), user).run();
                    case 6 -> storeData();
                    case 7 -> parameters();
                    case 8 -> UserAdminMenu.changeOwnPassword(in, out, app.auth(), user);
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void overview() {
        StoreOverview o = app.dashboard().storeOverview(LocalDate.now());
        QueueOverview q = o.queues();
        out.title("Visão Geral - " + app.store().current().name());
        out.println("  ESTOQUE");
        out.println("    Produtos ativos ............ " + o.activeProducts());
        out.println("    Abaixo do mínimo ........... " + o.belowMinimum());
        out.println("    Sem estoque ................ " + o.outOfStock());
        out.println("    Valor em estoque ........... " + TerminalOutput.money(o.stockValue()));
        out.println("  VENDAS DE HOJE");
        out.println("    Vendas ..................... " + o.salesOfDay().sales());
        out.println("    Unidades ................... " + o.salesOfDay().units());
        out.println("    Faturamento ................ " + TerminalOutput.money(o.salesOfDay().revenue()));
        out.println("    Ticket médio ............... " + TerminalOutput.money(o.salesOfDay().averageTicket()));
        out.println("  MAIS VENDIDOS (geral)");
        if (o.topSelling().isEmpty()) {
            out.println("    (ainda não há vendas)");
        }
        int position = 1;
        for (TopSellingProduct t : o.topSelling()) {
            out.println(String.format("    %d. %-40s %5d un", position++,
                    TerminalOutput.cut(t.product().name(), 40), t.unitsSold()));
        }
        out.println("  ATENDIMENTO");
        out.println("    Caixas abertos ............. " + q.openCheckouts() + " de " + q.totalCheckouts());
        out.println("    Clientes aguardando ........ " + q.waitingCustomers());
        out.println("    Atendimentos ............... " + q.totalAttendances());
        out.println("    Espera média ............... " + TerminalOutput.duration(q.averageWait()));
        out.println("    Menor fila ................. " + q.shortestQueueCheckoutId().map(id -> "caixa " + id).orElse("-"));
    }

    private void parameters() {
        out.title("Parâmetros");
        int current = app.queues().queueCapacity();
        out.println("  Capacidade máxima de cada fila (caixas novos): " + current);
        out.println("  Caixas existentes mantêm a capacidade com que foram criados:");
        for (Checkout c : app.queues().checkouts()) {
            out.println("    caixa " + c.id() + ": " + c.queueCapacity());
        }
        if (!in.confirm("Alterar a capacidade para os próximos caixas?")) return;
        int capacity = in.readInt("Nova capacidade (1 a 1000): ", 1, 1000);
        app.queues().changeQueueCapacity(capacity);
        out.success("Capacidade alterada para " + capacity + ". Vale para os caixas criados a partir de agora.");
    }

    private void storeData() {
        Supermarket s = app.store().current();
        out.title("Dados do Supermercado");
        out.println("  Nome ....... " + s.name());
        out.println("  CNPJ ....... " + s.formattedCnpj());
        out.println("  Endereço ... " + (s.address().isEmpty() ? "-" : s.address()));
        out.println("  Telefone ... " + (s.phone().isEmpty() ? "-" : s.phone()));
        if (!in.confirm("Editar os dados?")) return;
        out.println("Deixe em branco para manter o valor atual.");
        String name = in.readLine("Nome [" + s.name() + "]: ");
        String cnpj = in.readLine("CNPJ [" + s.formattedCnpj() + "]: ");
        String address = in.readLine("Endereço [" + s.address() + "]: ");
        String phone = in.readLine("Telefone [" + s.phone() + "]: ");
        try {
            app.store().update(name.isEmpty() ? s.name() : name, cnpj.isEmpty() ? s.cnpj() : cnpj,
                    address.isEmpty() ? s.address() : address, phone.isEmpty() ? s.phone() : phone);
            out.success("Dados do supermercado atualizados.");
        } catch (IllegalArgumentException e) {
            out.error(e.getMessage() + " Nada foi alterado.");
        }
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
                        out.println(SaleViews.receipt(sale, app.store().receiptHeader()));
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
