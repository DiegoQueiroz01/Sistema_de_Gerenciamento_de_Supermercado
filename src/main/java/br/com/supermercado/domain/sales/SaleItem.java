package br.com.supermercado.domain.sales;

// Item de uma venda. Guarda o nome e o preço do momento da venda,
// assim o cupom não muda se o produto for editado depois.
public class SaleItem {
    private final String productCode;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public SaleItem(String productCode, String productName, int quantity, double unitPrice) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("O código do produto do item é obrigatório.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("O nome do produto do item é obrigatório.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
        }
        if (Double.isNaN(unitPrice) || Double.isInfinite(unitPrice) || unitPrice <= 0) {
            throw new IllegalArgumentException("O preço unitário deve ser maior que zero.");
        }
        this.productCode = productCode.trim();
        this.productName = productName.trim();
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
