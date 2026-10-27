package br.com.supermercado.repositories;

import br.com.supermercado.domain.store.Supermarket;

import java.util.Optional;

/** Guarda os dados do único estabelecimento gerenciado pelo sistema. */
public interface SupermarketRepository {
    Optional<Supermarket> find();

    void save(Supermarket supermarket);
}
