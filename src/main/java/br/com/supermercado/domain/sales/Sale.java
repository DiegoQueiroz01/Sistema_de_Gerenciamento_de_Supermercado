package br.com.supermercado.domain.sales;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Venda finalizada: agrupa um ou mais itens. É imutável depois de criada.
 * O caixa e a senha do cliente são opcionais (venda feita fora de uma fila).
 */
public final class Sale {
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
        Objects.requireNonNull(dateTime, "A data da venda é obrigatória.");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Uma venda precisa ter ao menos um item.");
        }
        this.id = id;
        this.dateTime = dateTime;
        this.cashier = cashier == null || cashier.isBlank() ? "-" : cashier.trim();
        this.checkoutId = blankToNull(checkoutId);
        this.customerTicket = blankToNull(customerTicket);
        this.items = List.copyOf(items);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    public long id() { return id; }
    public LocalDateTime dateTime() { return dateTime; }
    public String cashier() { return cashier; }
    public Optional<String> checkoutId() { return Optional.ofNullable(checkoutId); }
    public Optional<String> customerTicket() { return Optional.ofNullable(customerTicket); }
    public List<SaleItem> items() { return items; }

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
