package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Product;

import java.util.List;

// Escolher um produto pelo código ou por parte do nome
class ProductSelector {
    private final InputReader in;
    private final TerminalOutput out;
    private final ProductService products;

    ProductSelector(InputReader in, TerminalOutput out, ProductService products) {
        this.in = in;
        this.out = out;
        this.products = products;
    }

    // Devolve o produto escolhido ou null se não achou / cancelou
    Product select(String prompt) {
        String term = in.readRequired(prompt);
        Product exact = products.findByCode(term);
        if (exact != null) {
            return exact;
        }
        List<Product> found = products.searchByName(term);
        if (found.isEmpty()) {
            out.error("Produto não encontrado.");
            return null;
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        // achou vários: mostra uma lista numerada para escolher
        for (int i = 0; i < found.size(); i++) {
            Product p = found.get(i);
            out.println("  " + (i + 1) + ") " + p.code() + " - " + p.name() + " | "
                    + TerminalOutput.money(p.price()) + " | " + p.quantity() + " un");
        }
        out.println("  0) Cancelar");
        int n = in.readInt("Escolha o produto: ", 0, found.size());
        if (n == 0) {
            return null;
        }
        return found.get(n - 1);
    }
}
