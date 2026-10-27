package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.checkout.ServiceRecord;
import br.com.supermercado.repositories.ServiceRecordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista em ordem de registro (histórico geral) + índice por caixa
 * (mapa de número do caixa para a lista dos seus atendimentos).
 */
public final class InMemoryServiceRecordRepository implements ServiceRecordRepository {
    private final List<ServiceRecord> records = new ArrayList<>();
    private final Map<String, List<ServiceRecord>> recordsByCheckout = new HashMap<>();
    private long nextId = 1;

    @Override
    public long nextId() {
        return nextId++;
    }

    @Override
    public void save(ServiceRecord record) {
        records.add(record);
        recordsByCheckout.computeIfAbsent(record.checkoutId(), id -> new ArrayList<>()).add(record);
    }

    @Override
    public List<ServiceRecord> findAll() {
        return List.copyOf(records);
    }

    @Override
    public List<ServiceRecord> findByCheckoutId(String checkoutId) {
        return List.copyOf(recordsByCheckout.getOrDefault(checkoutId, List.of()));
    }
}
