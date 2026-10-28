package br.com.supermercado.domain.product;

// Categoria do produto (Bebidas, Limpeza...)
public class Category {
    private final String name;

    public Category(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria não pode ser vazio.");
        }
        this.name = name.trim();
    }

    public String name() {
        return name;
    }
}
