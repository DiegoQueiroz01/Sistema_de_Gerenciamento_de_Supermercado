package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.checkout.ServiceRecord;
import br.com.supermercado.repositories.ServiceRecordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Lista com todos os atendimentos (histórico geral) e um mapa
// que guarda, para cada caixa, a lista dos atendimentos dele.
public class InMemoryServiceRecordRepository implements ServiceRecordRepository {
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
        List<ServiceRecord> list = recordsByCheckout.get(record.checkoutId());
        if (list == null) {
            list = new ArrayList<>();
            recordsByCheckout.put(record.checkoutId(), list);
        }
        list.add(record);
    }

    @Override
    public List<ServiceRecord> findAll() {
        return new ArrayList<>(records);
    }

    @Override
    public List<ServiceRecord> findByCheckoutId(String checkoutId) {
        List<ServiceRecord> list = recordsByCheckout.get(checkoutId);
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }
}
