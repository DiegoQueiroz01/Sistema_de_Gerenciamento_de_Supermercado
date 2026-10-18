package br.com.supermercado.repositories;

import br.com.supermercado.domain.inventory.StockMovement;

import java.util.List;

public interface StockMovementRepository {
    long nextId();

    void save(StockMovement movement);

    /** Todas as movimentações, em ordem de registro (mais antiga primeiro). */
    List<StockMovement> findAll();

    List<StockMovement> findByProductCode(String productCode);
}
