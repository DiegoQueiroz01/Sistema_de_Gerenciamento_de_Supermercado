package br.com.supermercado.application.product;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Operações de produtos e categorias (E.1)
public class ProductService {

    // Formas de ordenar a lista de produtos
    public enum SortCriterion {
        NAME, PRICE, QUANTITY
    }

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    // Tira os acentos e deixa em minúsculas, para buscar "ype" e achar "Ypê"
    public static String normalize(String text) {
        if (text == null) {
            text = "";
        }
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.toLowerCase();
    }

    // ---------- categorias ----------

    public Category registerCategory(String name) {
        Category category = new Category(name);
        if (findCategory(category.name()) != null) {
            throw new IllegalStateException("Já existe a categoria '" + category.name() + "'.");
        }
        products.saveCategory(category);
        return category;
    }

    // Devolve null se não encontrar
    public Category findCategory(String name) {
        String key = normalize(name);
        for (Category c : products.findAllCategories()) {
            if (normalize(c.name()).equals(key)) {
                return c;
            }
        }
        return null;
    }

    public List<Category> listCategories() {
        List<Category> list = products.findAllCategories();
        list.sort((a, b) -> normalize(a.name()).compareTo(normalize(b.name())));
        return list;
    }

    // ---------- produtos ----------

    // O produto é cadastrado com estoque zero; o estoque inicial entra depois como ENTRADA
    public Product registerProduct(String code, String name, Category category, double price,
                                   int minimumStock, LocalDateTime registeredAt) {
        Product product = new Product(code, name, category, price, minimumStock, registeredAt);
        if (products.existsByCode(product.code())) {
            throw new IllegalStateException("Código já cadastrado no sistema: " + product.code());
        }
        if (!isRegistered(category)) {
            throw new IllegalArgumentException("A categoria do produto não está cadastrada.");
        }
        products.save(product);
        return product;
    }

    private boolean isRegistered(Category category) {
        for (Category c : products.findAllCategories()) {
            if (c.name().equals(category.name())) {
                return true;
            }
        }
        return false;
    }

    // Devolve null se não encontrar
    public Product findByCode(String code) {
        return products.findByCode(code);
    }

    // Igual ao findByCode, mas dá erro se não encontrar
    public Product requireByCode(String code) {
        Product p = products.findByCode(code);
        if (p == null) {
            throw new IllegalArgumentException("Produto não encontrado: " + code);
        }
        return p;
    }

    // Busca por parte do nome, sem ligar para acentos e maiúsculas
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
            if (p.category().name().equals(category.name())) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> listSorted(SortCriterion criterion, boolean descending) {
        List<Product> list = products.findAll();
        switch (criterion) {
            case NAME:
                list.sort((a, b) -> normalize(a.name()).compareTo(normalize(b.name())));
                break;
            case PRICE:
                list.sort((a, b) -> Double.compare(a.price(), b.price()));
                break;
            case QUANTITY:
                list.sort((a, b) -> Integer.compare(a.quantity(), b.quantity()));
                break;
        }
        if (descending) {
            Collections.reverse(list);
        }
        return list;
    }

    public List<Product> listAll() {
        return products.findAll();
    }

    public void edit(String code, String name, Category category, double price, int minimumStock) {
        Product product = requireByCode(code);
        product.edit(name, category, price, minimumStock);
        products.save(product);
    }

    // Desativa o produto: ele some das consultas e das vendas, mas o histórico continua.
    // Só pode desativar com estoque zerado, para nenhum saldo sumir sem uma saída registrada.
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
