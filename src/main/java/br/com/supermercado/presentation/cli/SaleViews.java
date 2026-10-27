package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.application.sales.SaleService.SalesSummary;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.domain.sales.SaleItem;

import java.util.List;

// Monta os textos do carrinho, do cupom e da tabela de vendas
class SaleViews {
    private static final String ITEM_FORMAT = "  %-3s %-34s %5s %12s %12s%n";

    static String cart(Cart cart) {
        if (cart.isEmpty()) {
            return "  (carrinho vazio)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(ITEM_FORMAT, "#", "PRODUTO", "QTD", "UNITÁRIO", "SUBTOTAL"));
        sb.append(line('-', 70));
        int n = 1;
        for (Cart.Line line : cart.lines()) {
            sb.append(String.format(ITEM_FORMAT, n, TerminalOutput.cut(line.productName(), 34), line.quantity(),
                    TerminalOutput.money(line.unitPrice()), TerminalOutput.money(line.subtotal())));
            n++;
        }
        sb.append(line('-', 70));
        sb.append(String.format("  %d item(ns), %d unidade(s)   TOTAL: %s",
                cart.lines().size(), cart.totalUnits(), TerminalOutput.money(cart.total())));
        return sb.toString();
    }

    static String receipt(Sale sale, String storeHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append(line('=', 70));
        if (storeHeader != null && !storeHeader.isBlank()) {
            for (String text : storeHeader.split("\n")) {
                sb.append("  ").append(text).append('\n');
            }
            sb.append(line('-', 70));
        }
        sb.append(String.format("  CUPOM Nº %06d   %s   Operador: %s%n", sale.id(),
                TerminalOutput.dateTime(sale.dateTime()), sale.cashier()));
        if (sale.checkoutId() != null || sale.customerTicket() != null) {
            sb.append("  Caixa: ").append(orDash(sale.checkoutId()))
                    .append("   Cliente: ").append(orDash(sale.customerTicket())).append('\n');
        }
        sb.append(line('-', 70));
        sb.append(String.format(ITEM_FORMAT, "#", "PRODUTO", "QTD", "UNITÁRIO", "SUBTOTAL"));
        int n = 1;
        for (SaleItem item : sale.items()) {
            sb.append(String.format(ITEM_FORMAT, n, TerminalOutput.cut(item.productName(), 34), item.quantity(),
                    TerminalOutput.money(item.unitPrice()), TerminalOutput.money(item.subtotal())));
            n++;
        }
        sb.append(line('-', 70));
        sb.append(String.format("  %d unidade(s)%45s%n", sale.totalUnits(),
                "TOTAL: " + TerminalOutput.money(sale.total())));
        sb.append("  ").append("=".repeat(70));
        return sb.toString();
    }

    static String table(List<Sale> sales, SalesSummary summary) {
        if (sales.isEmpty()) {
            return "  (nenhuma venda registrada)";
        }
        StringBuilder sb = new StringBuilder();
        String format = "  %-8s %-16s %-6s %-7s %-20s %5s %12s%n";
        sb.append(String.format(format, "VENDA", "DATA", "CAIXA", "SENHA", "OPERADOR", "UN", "TOTAL"));
        sb.append(line('-', 82));
        for (Sale s : sales) {
            sb.append(String.format(format, String.format("%06d", s.id()), TerminalOutput.dateTime(s.dateTime()),
                    orDash(s.checkoutId()), orDash(s.customerTicket()), TerminalOutput.cut(s.cashier(), 20),
                    s.totalUnits(), TerminalOutput.money(s.total())));
        }
        sb.append(line('-', 82));
        sb.append(String.format("  Vendas: %d | Unidades: %d | Faturamento: %s | Ticket médio: %s",
                summary.sales(), summary.units(), TerminalOutput.money(summary.revenue()),
                TerminalOutput.money(summary.averageTicket())));
        return sb.toString();
    }

    // Linha separadora, por exemplo "  ------"
    private static String line(char c, int size) {
        return "  " + String.valueOf(c).repeat(size) + "\n";
    }

    private static String orDash(String text) {
        if (text == null) {
            return "-";
        }
        return text;
    }
}
