package br.com.supermercado.application.product;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Casos de uso de produtos e categorias (E.1). Não depende de menus. */
public final class ProductService {

    /** Critérios de ordenação exigidos: nome, preço ou quantidade (via Comparator). */
    public enum SortCriterion {
        NAME(Comparator.comparing((Product p) -> normalize(p.name()))),
        PRICE(Comparator.comparingDouble(Product::price)),
        QUANTITY(Comparator.comparingInt(Product::quantity));

        private final Comparator<Product> comparator;

        SortCriterion(Comparator<Product> comparator) {
            this.comparator = comparator;
        }

        public Comparator<Product> comparator() {
            return comparator;
        }
    }

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = Objects.requireNonNull(products);
    }

    /** Remove acentos e converte para minúsculas (buscas e ordenação por nome). */
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    // ----------------------------------------------------------- categorias
    public Category registerCategory(String name) {
        Category category = new Category(name);
        if (findCategory(category.name()).isPresent()) {
            throw new IllegalStateException("Já existe a categoria '" + category.name() + "'.");
        }
        products.saveCategory(category);
        return category;
    }

    public Optional<Category> findCategory(String name) {
        String key = normalize(name);
        return products.findAllCategories().stream()
                .filter(c -> normalize(c.name()).equals(key)).findFirst();
    }

    public List<Category> listCategories() {
        List<Category> list = new ArrayList<>(products.findAllCategories());
        list.sort(Comparator.comparing(c -> normalize(c.name())));
        return list;
    }

    // ----------------------------------------------------------- produtos
    /** Cadastra produto sem estoque (o estoque inicial é uma ENTRY feita pelo InventoryService). */
    public Product registerProduct(String code, String name, Category category, double price,
                                   int minimumStock, LocalDateTime registeredAt) {
        Product product = new Product(code, name, category, price, minimumStock, registeredAt);
        if (products.existsByCode(product.code())) {
            throw new IllegalStateException("Código já cadastrado no sistema: " + product.code());
        }
        if (!products.findAllCategories().contains(category)) {
            throw new IllegalArgumentException("A categoria do produto não está cadastrada.");
        }
        products.save(product);
        return product;
    }

    public Optional<Product> findByCode(String code) {
        return products.findByCode(code);
    }

    public Product requireByCode(String code) {
        return products.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + code));
    }

    /** Busca por parte do nome, ignorando maiúsculas e acentos. */
    public List<Product> searchByName(String name) {
        String key = normalize(name);
        List<Product> result = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (normalize(p.name()).contains(key)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> filterByCategory(Category category) {
        List<Product> result = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (p.category().equals(category)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> listSorted(SortCriterion criterion, boolean descending) {
        List<Product> copy = new ArrayList<>(products.findAll());
        Comparator<Product> comparator = criterion.comparator();
        copy.sort(descending ? comparator.reversed() : comparator);
        return copy;
    }

    public List<Product> listAll() {
        return products.findAll();
    }

    public void edit(String code, String name, Category category, double price, int minimumStock) {
        Product product = requireByCode(code);
        product.edit(name, category, price, minimumStock);
        products.save(product);
    }

    /**
     * Desativa o produto: ele sai das consultas e não pode mais ser vendido, mas o histórico
     * de movimentações e vendas é mantido. Só é permitido com estoque zerado, para que nenhum
     * saldo "suma" sem uma saída registrada.
     */
    public Product deactivate(String code) {
        Product product = requireByCode(code);
        if (product.quantity() > 0) {
            throw new IllegalStateException("Zere o estoque de '" + product.name()
                    + "' com uma saída antes de desativá-lo (saldo atual: " + product.quantity() + ").");
        }
        products.deactivate(product.code());
        return product;
    }

    public void defineMinimumStock(String code, int minimumStock) {
        Product p = requireByCode(code);
        p.edit(p.name(), p.category(), p.price(), minimumStock);
        products.save(p);
    }
}
