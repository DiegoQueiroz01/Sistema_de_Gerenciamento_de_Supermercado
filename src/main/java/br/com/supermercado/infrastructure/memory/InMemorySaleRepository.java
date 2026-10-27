package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.repositories.SaleRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lista em ordem de registro (histórico de vendas) + mapa por número da venda
 * para consulta direta de um cupom.
 */
public final class InMemorySaleRepository implements SaleRepository {
    private final List<Sale> sales = new ArrayList<>();
    private final Map<Long, Sale> salesById = new HashMap<>();
    private long nextId = 1;

    @Override
    public long nextId() {
        return nextId++;
    }

    @Override
    public void save(Sale sale) {
        if (salesById.containsKey(sale.id())) {
            throw new IllegalStateException("Venda já registrada: " + sale.id());
        }
        sales.add(sale);
        salesById.put(sale.id(), sale);
    }

    @Override
    public Optional<Sale> findById(long id) {
        return Optional.ofNullable(salesById.get(id));
    }

    @Override
    public List<Sale> findAll() {
        return List.copyOf(sales);
    }
}
