package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.repositories.CheckoutRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mapa com o número do caixa como chave. O LinkedHashMap mantém a ordem
// de criação, que é usada para percorrer os caixas e achar a menor fila.
public class InMemoryCheckoutRepository implements CheckoutRepository {
    private final Map<String, Checkout> checkouts = new LinkedHashMap<>();

    @Override
    public void save(Checkout checkout) {
        checkouts.put(checkout.id(), checkout);
    }

    @Override
    public Checkout findById(String id) {
        if (id == null) {
            return null;
        }
        return checkouts.get(id.trim());
    }

    @Override
    public List<Checkout> findAll() {
        return new ArrayList<>(checkouts.values());
    }

    @Override
    public void remove(String id) {
        checkouts.remove(id);
    }
}
