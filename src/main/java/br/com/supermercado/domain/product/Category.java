package br.com.supermercado.domain.product;

/** Grupo ao qual um produto pertence (Bebidas, Limpeza...). */
public record Category(String name) {
    public Category {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria não pode ser vazio.");
        }
        name = name.trim();
    }
}
