package br.com.supermercado.repositories;

import br.com.supermercado.domain.checkout.Checkout;

import java.util.List;
import java.util.Optional;

public interface CheckoutRepository {
    void save(Checkout checkout);

    Optional<Checkout> findById(String id);

    /** Caixas na ordem em que foram criados. */
    List<Checkout> findAll();

    void remove(String id);
}
