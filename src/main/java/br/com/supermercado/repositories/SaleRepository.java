package br.com.supermercado.repositories;

import br.com.supermercado.domain.sales.Sale;

import java.util.List;

public interface SaleRepository {
    long nextId();

    void save(Sale sale);

    // Devolve null se a venda não existir
    Sale findById(long id);

    // Vendas na ordem em que foram registradas (mais antiga primeiro)
    List<Sale> findAll();
}
