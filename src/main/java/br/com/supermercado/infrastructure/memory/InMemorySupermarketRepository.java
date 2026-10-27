package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.repositories.SupermarketRepository;

// Guarda só um registro: os dados do supermercado
public class InMemorySupermarketRepository implements SupermarketRepository {
    private Supermarket supermarket;

    @Override
    public Supermarket find() {
        return supermarket;
    }

    @Override
    public void save(Supermarket supermarket) {
        if (supermarket == null) {
            throw new IllegalArgumentException("Os dados do supermercado são obrigatórios.");
        }
        this.supermarket = supermarket;
    }
}
