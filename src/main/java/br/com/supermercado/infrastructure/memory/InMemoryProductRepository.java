package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Produtos ficam num mapa com o código como chave: a busca pelo código é direta
// e não deixa cadastrar código repetido. Os desativados vão para outro mapa.
public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Product> inactiveProducts = new LinkedHashMap<>();
    private final List<Category> categories = new ArrayList<>();

    @Override
    public void save(Product product) {
        inactiveProducts.remove(product.code());
        products.put(product.code(), product);
    }

    @Override
    public void deactivate(String code) {
        if (code == null) {
            return;
        }
        Product removed = products.remove(code.trim());
        if (removed != null) {
            inactiveProducts.put(removed.code(), removed);
        }
    }

    @Override
    public Product findByCode(String code) {
        if (code == null) {
            return null;
        }
        return products.get(code.trim());
    }

    @Override
    public boolean existsByCode(String code) {
        return code != null && products.containsKey(code.trim());
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void saveCategory(Category category) {
        categories.add(category);
    }

    @Override
    public List<Category> findAllCategories() {
        return new ArrayList<>(categories);
    }
}
