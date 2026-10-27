package br.com.supermercado.domain.sales;

/**
 * Linha de uma venda. Guarda o nome e o preço do produto no momento da venda,
 * para que o cupom não mude se o cadastro do produto for editado depois.
 */
public record SaleItem(String productCode, String productName, int quantity, double unitPrice) {

    public SaleItem {
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
        productCode = productCode.trim();
        productName = productName.trim();
    }

    public double subtotal() {
        return quantity * unitPrice;
    }
}
