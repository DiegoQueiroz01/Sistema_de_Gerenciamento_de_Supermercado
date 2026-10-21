package br.com.supermercado.repositories;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void save(Product product);

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    List<Product> findAll();

    void saveCategory(Category category);

    List<Category> findAllCategories();
}
