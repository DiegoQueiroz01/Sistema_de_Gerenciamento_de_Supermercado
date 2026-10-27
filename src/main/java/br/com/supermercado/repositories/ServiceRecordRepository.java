package br.com.supermercado.repositories;

import br.com.supermercado.domain.checkout.ServiceRecord;

import java.util.List;

public interface ServiceRecordRepository {
    long nextId();

    void save(ServiceRecord record);

    /** Todos os atendimentos, em ordem de registro (mais antigo primeiro). */
    List<ServiceRecord> findAll();

    List<ServiceRecord> findByCheckoutId(String checkoutId);
}
