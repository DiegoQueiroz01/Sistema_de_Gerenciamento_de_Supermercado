package br.com.supermercado.domain.product;

import br.com.supermercado.domain.inventory.MovementType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Mercadoria cadastrada. A quantidade em estoque só muda por {@link #updateStock},
 * ou seja, por meio de movimentações (entrada/saída).
 */
public final class Product {
    private final String code;
    private String name;
    private Category category;
    private double price;
    private int quantity;
    private int minimumStock;
    private final LocalDateTime registeredAt;

    public Product(String code, String name, Category category, double price,
                   int minimumStock, LocalDateTime registeredAt) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("O código do produto não pode ser vazio.");
        }
        Objects.requireNonNull(registeredAt, "A data de cadastro é obrigatória.");
        this.code = code.trim();
        this.registeredAt = registeredAt;
        this.quantity = 0; // o estoque inicial entra como movimentação de ENTRY
        edit(name, category, price, minimumStock);
    }

    /** Altera dados cadastrais (inclusive estoque mínimo). Valida tudo antes de alterar. */
    public void edit(String name, Category category, double price, int minimumStock) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do produto não pode ser vazio.");
        }
        if (category == null) {
            throw new IllegalArgumentException("O produto precisa de uma categoria.");
        }
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        if (minimumStock < 0) {
            throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        }
        this.name = name.trim();
        this.category = category;
        this.price = price;
        this.minimumStock = minimumStock;
    }

    /** Atualiza a quantidade imediatamente; impede saída maior que o estoque disponível. */
    public void updateStock(int amount, MovementType type) {
        if (amount <= 0) {
            throw new IllegalArgumentException("A quantidade da movimentação deve ser maior que zero.");
        }
        Objects.requireNonNull(type, "O tipo da movimentação é obrigatório.");
        if (type == MovementType.EXIT) {
            if (amount > quantity) {
                throw new IllegalStateException("Quantidade indisponível em estoque para '" + name
                        + "' (solicitado: " + amount + ", máx: " + quantity + ").");
            }
            quantity -= amount;
        } else {
            quantity += amount;
        }
    }

    /** Abaixo do mínimo = quantidade estritamente menor que o estoque mínimo. */
    public boolean isBelowMinimum() {
        return quantity < minimumStock;
    }

    public String code() { return code; }
    public String name() { return name; }
    public Category category() { return category; }
    public double price() { return price; }
    public int quantity() { return quantity; }
    public int minimumStock() { return minimumStock; }
    public LocalDateTime registeredAt() { return registeredAt; }
}
