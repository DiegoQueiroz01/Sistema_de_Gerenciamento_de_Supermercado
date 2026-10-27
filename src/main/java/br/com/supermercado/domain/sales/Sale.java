package br.com.supermercado.domain.sales;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Venda finalizada. O caixa e a senha do cliente podem ficar null
// quando a venda é feita sem fila.
public class Sale {
    private final long id;
    private final LocalDateTime dateTime;
    private final String cashier;
    private final String checkoutId;
    private final String customerTicket;
    private final List<SaleItem> items;

    public Sale(long id, LocalDateTime dateTime, String cashier, String checkoutId,
                String customerTicket, List<SaleItem> items) {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador da venda deve ser positivo.");
        }
        if (dateTime == null) {
            throw new IllegalArgumentException("A data da venda é obrigatória.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Uma venda precisa ter ao menos um item.");
        }
        this.id = id;
        this.dateTime = dateTime;
        if (cashier == null || cashier.isBlank()) {
            this.cashier = "-";
        } else {
            this.cashier = cashier.trim();
        }
        this.checkoutId = emptyToNull(checkoutId);
        this.customerTicket = emptyToNull(customerTicket);
        this.items = new ArrayList<>(items);
    }

    private static String emptyToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    public long id() { return id; }
    public LocalDateTime dateTime() { return dateTime; }
    public String cashier() { return cashier; }
    public String checkoutId() { return checkoutId; }
    public String customerTicket() { return customerTicket; }

    public List<SaleItem> items() {
        return new ArrayList<>(items);
    }

    public double total() {
        double total = 0;
        for (SaleItem item : items) {
            total += item.subtotal();
        }
        return total;
    }

    public int totalUnits() {
        int units = 0;
        for (SaleItem item : items) {
            units += item.quantity();
        }
        return units;
    }
}
