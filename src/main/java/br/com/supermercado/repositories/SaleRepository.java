package br.com.supermercado.repositories;

import br.com.supermercado.domain.sales.Sale;

import java.util.List;
import java.util.Optional;

public interface SaleRepository {
    long nextId();

    void save(Sale sale);

    Optional<Sale> findById(long id);

    /** Todas as vendas, em ordem de registro (mais antiga primeiro). */
    List<Sale> findAll();
}
