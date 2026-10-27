package br.com.supermercado.application.sales;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Product;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Venda em montagem (antes de ser finalizada). Estruturas usadas:
 * - mapa ordenado por código: soma itens repetidos do mesmo produto e mantém a ordem de inclusão;
 * - pilha de inclusões: permite "desfazer o último item" na ordem inversa (LIFO).
 * O estoque é conferido a cada inclusão, mas só é baixado quando a venda é finalizada.
 */
public final class Cart {

    /** Linha do carrinho com o preço vigente do produto. */
    public record Line(String productCode, String productName, int quantity, double unitPrice) {
        public double subtotal() {
            return quantity * unitPrice;
        }
    }

    private record Addition(String productCode, int quantity) { }

    private final ProductService products;
    private final InventoryService inventory;
    private final Map<String, Line> lines = new LinkedHashMap<>();
    private final Deque<Addition> additions = new ArrayDeque<>();

    Cart(ProductService products, InventoryService inventory) {
        this.products = Objects.requireNonNull(products);
        this.inventory = Objects.requireNonNull(inventory);
    }

    /** Inclui um produto. Recusa se o total no carrinho passar do estoque disponível. */
    public Line add(String productCode, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
        }
        Product product = products.requireByCode(productCode);
        int alreadyInCart = quantityOf(product.code());
        if (!inventory.hasStock(product.code(), alreadyInCart + quantity)) {
            throw new IllegalStateException("Quantidade indisponível em estoque para '" + product.name()
                    + "' (no carrinho: " + alreadyInCart + ", disponível: " + product.quantity() + ").");
        }
        Line line = new Line(product.code(), product.name(), alreadyInCart + quantity, product.price());
        lines.put(product.code(), line);
        additions.push(new Addition(product.code(), quantity));
        return line;
    }

    /** Desfaz a última inclusão (pilha). Devolve a linha afetada ou null se o carrinho estiver vazio. */
    public Line undoLast() {
        Addition last = additions.poll();
        if (last == null) {
            return null;
        }
        Line line = lines.get(last.productCode());
        int remaining = line.quantity() - last.quantity();
        Line undone = new Line(line.productCode(), line.productName(), last.quantity(), line.unitPrice());
        if (remaining > 0) {
            lines.put(line.productCode(), new Line(line.productCode(), line.productName(), remaining,
                    line.unitPrice()));
        } else {
            lines.remove(line.productCode());
        }
        return undone;
    }

    /** Retira todas as unidades de um produto do carrinho. */
    public boolean remove(String productCode) {
        Line removed = lines.remove(productCode == null ? null : productCode.trim());
        if (removed == null) {
            return false;
        }
        additions.removeIf(a -> a.productCode().equals(removed.productCode()));
        return true;
    }

    public int quantityOf(String productCode) {
        Line line = lines.get(productCode);
        return line == null ? 0 : line.quantity();
    }

    public List<Line> lines() {
        return new ArrayList<>(lines.values());
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int totalUnits() {
        int units = 0;
        for (Line line : lines.values()) {
            units += line.quantity();
        }
        return units;
    }

    public double total() {
        double total = 0;
        for (Line line : lines.values()) {
            total += line.subtotal();
        }
        return total;
    }
}
