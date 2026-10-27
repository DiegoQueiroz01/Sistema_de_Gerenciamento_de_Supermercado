package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.repositories.SupermarketRepository;

import java.util.Objects;
import java.util.Optional;

/** Um único registro (o estabelecimento). */
public final class InMemorySupermarketRepository implements SupermarketRepository {
    private Supermarket supermarket;

    @Override
    public Optional<Supermarket> find() {
        return Optional.ofNullable(supermarket);
    }

    @Override
    public void save(Supermarket supermarket) {
        this.supermarket = Objects.requireNonNull(supermarket);
    }
}
