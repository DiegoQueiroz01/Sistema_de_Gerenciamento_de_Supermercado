package br.com.supermercado.application.store;

import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.repositories.SupermarketRepository;

import java.util.Objects;

/** Consulta e atualização dos dados cadastrais do estabelecimento. */
public final class SupermarketService {
    public static final String DEFAULT_NAME = "Mercado Fácil";

    private final SupermarketRepository repository;

    public SupermarketService(SupermarketRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /** Dados atuais; se nada foi cadastrado ainda, usa só o nome padrão. */
    public Supermarket current() {
        return repository.find().orElseGet(() -> new Supermarket(DEFAULT_NAME, "", "", ""));
    }

    public Supermarket update(String name, String cnpj, String address, String phone) {
        Supermarket supermarket = new Supermarket(name, cnpj, address, phone);
        repository.save(supermarket);
        return supermarket;
    }

    /** Linhas do cabeçalho do cupom. */
    public String receiptHeader() {
        Supermarket s = current();
        StringBuilder sb = new StringBuilder(s.name().toUpperCase());
        if (!s.cnpj().isEmpty()) sb.append("\nCNPJ: ").append(s.formattedCnpj());
        if (!s.address().isEmpty()) sb.append("\n").append(s.address());
        if (!s.phone().isEmpty()) sb.append("\nTel.: ").append(s.phone());
        return sb.toString();
    }
}
