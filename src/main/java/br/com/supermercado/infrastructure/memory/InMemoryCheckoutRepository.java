package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.repositories.CheckoutRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapa por número do caixa que preserva a ordem de criação: busca direta pelo número
 * e percurso em ordem para localizar a menor fila e consolidar métricas.
 */
public final class InMemoryCheckoutRepository implements CheckoutRepository {
    private final Map<String, Checkout> checkoutsById = new LinkedHashMap<>();

    @Override
    public void save(Checkout checkout) {
        checkoutsById.put(checkout.id(), checkout);
    }

    @Override
    public Optional<Checkout> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(checkoutsById.get(id.trim()));
    }

    @Override
    public List<Checkout> findAll() {
        return List.copyOf(checkoutsById.values());
    }

    @Override
    public void remove(String id) {
        checkoutsById.remove(id);
    }
}
