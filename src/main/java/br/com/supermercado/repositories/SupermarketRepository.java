package br.com.supermercado.repositories;

import br.com.supermercado.domain.store.Supermarket;

// Guarda os dados do supermercado (só existe um)
public interface SupermarketRepository {
    // Devolve null se ainda não foi cadastrado
    Supermarket find();

    void save(Supermarket supermarket);
}
