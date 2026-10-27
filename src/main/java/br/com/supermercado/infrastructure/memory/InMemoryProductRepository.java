package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapa por código (busca O(1) e prevenção de duplicados) + lista de categorias.
 * Produtos desativados saem do mapa principal e vão para um mapa à parte.
 */
public final class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> productsByCode = new LinkedHashMap<>();
    private final Map<String, Product> inactiveByCode = new LinkedHashMap<>();
    private final List<Category> categories = new ArrayList<>();

    @Override
    public void save(Product product) {
        inactiveByCode.remove(product.code());
        productsByCode.put(product.code(), product);
    }

    @Override
    public void deactivate(String code) {
        Product removed = code == null ? null : productsByCode.remove(code.trim());
        if (removed != null) {
            inactiveByCode.put(removed.code(), removed);
        }
    }

    @Override
    public Optional<Product> findByCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(productsByCode.get(code.trim()));
    }

    @Override
    public boolean existsByCode(String code) {
        return code != null && productsByCode.containsKey(code.trim());
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(productsByCode.values());
    }

    @Override
    public void saveCategory(Category category) {
        categories.add(category);
    }

    @Override
    public List<Category> findAllCategories() {
        return List.copyOf(categories);
    }
}
