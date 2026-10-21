package br.com.supermercado.domain.inventory;

import java.time.LocalDateTime;
import java.util.Objects;

/** Registro imutável de uma entrada ou saída de estoque (histórico). */
public record StockMovement(
        long id,
        String productCode,
        int quantity,
        MovementType type,
        LocalDateTime timestamp,
        String responsible) {

    public StockMovement {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador da movimentação deve ser positivo.");
        }
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("O código do produto é obrigatório.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade da movimentação deve ser maior que zero.");
        }
        Objects.requireNonNull(type, "O tipo da movimentação é obrigatório.");
        Objects.requireNonNull(timestamp, "A data da movimentação é obrigatória.");
    }
}
