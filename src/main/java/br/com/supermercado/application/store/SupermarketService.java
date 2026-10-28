package br.com.supermercado.application.store;

import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.repositories.SupermarketRepository;

// Consulta e alteração dos dados do supermercado
public class SupermarketService {
    public static final String DEFAULT_NAME = "Mercado Fácil";

    private final SupermarketRepository repository;

    public SupermarketService(SupermarketRepository repository) {
        this.repository = repository;
    }

    // Se nada foi cadastrado ainda, usa só o nome padrão
    public Supermarket current() {
        Supermarket s = repository.find();
        if (s == null) {
            return new Supermarket(DEFAULT_NAME, "", "", "");
        }
        return s;
    }

    public Supermarket update(String name, String cnpj, String address, String phone) {
        Supermarket supermarket = new Supermarket(name, cnpj, address, phone);
        repository.save(supermarket);
        return supermarket;
    }

    // Linhas do topo do cupom
    public String receiptHeader() {
        Supermarket s = current();
        String header = s.name().toUpperCase();
        if (!s.cnpj().isEmpty()) {
            header += "\nCNPJ: " + s.formattedCnpj();
        }
        if (!s.address().isEmpty()) {
            header += "\n" + s.address();
        }
        if (!s.phone().isEmpty()) {
            header += "\nTel.: " + s.phone();
        }
        return header;
    }
}
