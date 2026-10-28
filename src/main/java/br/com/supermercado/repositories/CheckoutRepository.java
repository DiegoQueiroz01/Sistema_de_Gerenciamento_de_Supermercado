package br.com.supermercado.repositories;

import br.com.supermercado.domain.checkout.Checkout;

import java.util.List;

public interface CheckoutRepository {
    void save(Checkout checkout);

    // Devolve null se o caixa não existir
    Checkout findById(String id);

    // Caixas na ordem em que foram criados
    List<Checkout> findAll();

    void remove(String id);
}
