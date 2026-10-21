package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.repositories.StockMovementRepository;

import java.util.ArrayList;
import java.util.List;

/** Lista em ordem de registro (histórico). */
public final class InMemoryStockMovementRepository implements StockMovementRepository {
    private final List<StockMovement> movements = new ArrayList<>();
    private long nextId = 1;

    @Override
    public long nextId() {
        return nextId++;
    }

    @Override
    public void save(StockMovement movement) {
        movements.add(movement);
    }

    @Override
    public List<StockMovement> findAll() {
        return List.copyOf(movements);
    }

    @Override
    public List<StockMovement> findByProductCode(String productCode) {
        List<StockMovement> result = new ArrayList<>();
        for (StockMovement m : movements) {
            if (m.productCode().equals(productCode)) {
                result.add(m);
            }
        }
        return result;
    }
}
