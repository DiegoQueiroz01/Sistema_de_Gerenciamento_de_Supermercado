package br.com.supermercado.repositories;

import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;

import java.util.List;

public interface ProductRepository {
    void save(Product product);

    // Devolve null se o produto não existir (ou estiver desativado)
    Product findByCode(String code);

    boolean existsByCode(String code);

    List<Product> findAll();

    // Tira o produto das consultas sem apagar o histórico.
    // Cadastrar o mesmo código de novo reativa o produto.
    void deactivate(String code);

    void saveCategory(Category category);

    List<Category> findAllCategories();
}
