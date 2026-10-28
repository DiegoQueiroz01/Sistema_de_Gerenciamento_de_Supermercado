package br.com.supermercado.application.sales;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Product;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Carrinho de uma venda que ainda não foi finalizada.
// - mapa por código: se o mesmo produto entra duas vezes, as quantidades somam;
// - pilha de inclusões: "desfazer último item" tira na ordem inversa (LIFO).
// O estoque é conferido a cada item, mas só baixa quando a venda é finalizada.
public class Cart {

    // Uma linha do carrinho
    public static class Line {
        private final String productCode;
        private final String productName;
        private final int quantity;
        private final double unitPrice;

        public Line(String productCode, String productName, int quantity, double unitPrice) {
            this.productCode = productCode;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String productCode() { return productCode; }
        public String productName() { return productName; }
        public int quantity() { return quantity; }
        public double unitPrice() { return unitPrice; }

        public double subtotal() {
            return quantity * unitPrice;
        }
    }

    // O que vai para a pilha: qual produto e quantas unidades foram adicionadas
    private static class Addition {
        String productCode;
        int quantity;

        Addition(String productCode, int quantity) {
            this.productCode = productCode;
            this.quantity = quantity;
        }
    }

    private final ProductService products;
    private final InventoryService inventory;
    private final Map<String, Line> lines = new LinkedHashMap<>();
    private final Deque<Addition> additions = new ArrayDeque<>(); // usada como pilha

    Cart(ProductService products, InventoryService inventory) {
        this.products = products;
        this.inventory = inventory;
    }

    // Adiciona um produto. Não deixa passar do estoque disponível.
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

    // Desfaz a última inclusão (topo da pilha). Devolve o que foi tirado ou null se estiver vazio.
    public Line undoLast() {
        if (additions.isEmpty()) {
            return null;
        }
        Addition last = additions.pop();
        Line line = lines.get(last.productCode);
        int remaining = line.quantity() - last.quantity;
        if (remaining > 0) {
            lines.put(line.productCode(), new Line(line.productCode(), line.productName(), remaining,
                    line.unitPrice()));
        } else {
            lines.remove(line.productCode());
        }
        return new Line(line.productCode(), line.productName(), last.quantity, line.unitPrice());
    }

    // Tira todas as unidades de um produto do carrinho
    public boolean remove(String productCode) {
        if (productCode == null) {
            return false;
        }
        Line removed = lines.remove(productCode.trim());
        if (removed == null) {
            return false;
        }
        // tira também da pilha, para o "desfazer" não voltar nesse produto
        Iterator<Addition> it = additions.iterator();
        while (it.hasNext()) {
            if (it.next().productCode.equals(removed.productCode())) {
                it.remove();
            }
        }
        return true;
    }

    public int quantityOf(String productCode) {
        Line line = lines.get(productCode);
        if (line == null) {
            return 0;
        }
        return line.quantity();
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
