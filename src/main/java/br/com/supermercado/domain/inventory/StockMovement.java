package br.com.supermercado.domain.inventory;

import java.time.LocalDateTime;

// Uma entrada ou saída de estoque (fica guardada no histórico)
public class StockMovement {
    private final long id;
    private final String productCode;
    private final int quantity;
    private final MovementType type;
    private final LocalDateTime timestamp;
    private final String responsible;

    public StockMovement(long id, String productCode, int quantity, MovementType type,
                         LocalDateTime timestamp, String responsible) {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador da movimentação deve ser positivo.");
        }
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("O código do produto é obrigatório.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade da movimentação deve ser maior que zero.");
        }
        if (type == null) {
            throw new IllegalArgumentException("O tipo da movimentação é obrigatório.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("A data da movimentação é obrigatória.");
        }
        this.id = id;
        this.productCode = productCode;
        this.quantity = quantity;
        this.type = type;
        this.timestamp = timestamp;
        this.responsible = responsible;
    }

    public long id() { return id; }
    public String productCode() { return productCode; }
    public int quantity() { return quantity; }
    public MovementType type() { return type; }
    public LocalDateTime timestamp() { return timestamp; }
    public String responsible() { return responsible; }
}
