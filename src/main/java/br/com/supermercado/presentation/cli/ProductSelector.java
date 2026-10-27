package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Product;

import java.util.List;
import java.util.Optional;

/** Escolha de produto no terminal: por código exato ou por parte do nome (lista numerada). */
final class ProductSelector {
    private final InputReader in;
    private final TerminalOutput out;
    private final ProductService products;

    ProductSelector(InputReader in, TerminalOutput out, ProductService products) {
        this.in = in;
        this.out = out;
        this.products = products;
    }

    /** Devolve o produto escolhido ou null se não encontrado/cancelado. */
    Product select(String prompt) {
        String term = in.readRequired(prompt);
        Optional<Product> exact = products.findByCode(term);
        if (exact.isPresent()) return exact.get();
        List<Product> found = products.searchByName(term);
        if (found.isEmpty()) {
            out.error("Produto não encontrado.");
            return null;
        }
        if (found.size() == 1) return found.get(0);
        for (int i = 0; i < found.size(); i++) {
            Product p = found.get(i);
            out.println("  " + (i + 1) + ") " + p.code() + " - " + p.name() + " | "
                    + TerminalOutput.money(p.price()) + " | " + p.quantity() + " un");
        }
        out.println("  0) Cancelar");
        int n = in.readInt("Escolha o produto: ", 0, found.size());
        return n == 0 ? null : found.get(n - 1);
    }
}
