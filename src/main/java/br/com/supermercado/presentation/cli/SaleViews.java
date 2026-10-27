package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.application.sales.SaleService.SalesSummary;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.domain.sales.SaleItem;

import java.util.List;

import static br.com.supermercado.presentation.cli.TerminalOutput.cut;
import static br.com.supermercado.presentation.cli.TerminalOutput.dateTime;
import static br.com.supermercado.presentation.cli.TerminalOutput.money;

/** Tabelas de vendas usadas pelo caixa e pelo administrador. */
final class SaleViews {
    private static final String ITEM_FORMAT = "  %-3s %-34s %5s %12s %12s%n";

    private SaleViews() {}

    static String cart(Cart cart) {
        if (cart.isEmpty()) return "  (carrinho vazio)";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(ITEM_FORMAT, "#", "PRODUTO", "QTD", "UNITÁRIO", "SUBTOTAL"));
        sb.append("  ").append("-".repeat(70)).append('\n');
        int n = 1;
        for (Cart.Line line : cart.lines()) {
            sb.append(String.format(ITEM_FORMAT, n++, cut(line.productName(), 34), line.quantity(),
                    money(line.unitPrice()), money(line.subtotal())));
        }
        sb.append("  ").append("-".repeat(70)).append('\n');
        sb.append(String.format("  %d item(ns), %d unidade(s)   TOTAL: %s",
                cart.lines().size(), cart.totalUnits(), money(cart.total())));
        return sb.toString();
    }

    static String receipt(Sale sale, String storeHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append("=".repeat(70)).append('\n');
        if (storeHeader != null && !storeHeader.isBlank()) {
            for (String line : storeHeader.split("\n")) {
                sb.append("  ").append(line).append('\n');
            }
            sb.append("  ").append("-".repeat(70)).append('\n');
        }
        sb.append(String.format("  CUPOM Nº %06d   %s   Operador: %s%n", sale.id(),
                dateTime(sale.dateTime()), sale.cashier()));
        if (sale.checkoutId().isPresent() || sale.customerTicket().isPresent()) {
            sb.append("  Caixa: ").append(sale.checkoutId().orElse("-"))
                    .append("   Cliente: ").append(sale.customerTicket().orElse("-")).append('\n');
        }
        sb.append("  ").append("-".repeat(70)).append('\n');
        sb.append(String.format(ITEM_FORMAT, "#", "PRODUTO", "QTD", "UNITÁRIO", "SUBTOTAL"));
        int n = 1;
        for (SaleItem item : sale.items()) {
            sb.append(String.format(ITEM_FORMAT, n++, cut(item.productName(), 34), item.quantity(),
                    money(item.unitPrice()), money(item.subtotal())));
        }
        sb.append("  ").append("-".repeat(70)).append('\n');
        sb.append(String.format("  %d unidade(s)%45s%n", sale.totalUnits(), "TOTAL: " + money(sale.total())));
        sb.append("  ").append("=".repeat(70));
        return sb.toString();
    }

    static String table(List<Sale> sales, SalesSummary summary) {
        if (sales.isEmpty()) return "  (nenhuma venda registrada)";
        StringBuilder sb = new StringBuilder();
        String format = "  %-8s %-16s %-6s %-7s %-20s %5s %12s%n";
        sb.append(String.format(format, "VENDA", "DATA", "CAIXA", "SENHA", "OPERADOR", "UN", "TOTAL"));
        sb.append("  ").append("-".repeat(82)).append('\n');
        for (Sale s : sales) {
            sb.append(String.format(format, String.format("%06d", s.id()), dateTime(s.dateTime()),
                    s.checkoutId().orElse("-"), s.customerTicket().orElse("-"), cut(s.cashier(), 20),
                    s.totalUnits(), money(s.total())));
        }
        sb.append("  ").append("-".repeat(82)).append('\n');
        sb.append(String.format("  Vendas: %d | Unidades: %d | Faturamento: %s | Ticket médio: %s",
                summary.sales(), summary.units(), money(summary.revenue()), money(summary.averageTicket())));
        return sb.toString();
    }
}
