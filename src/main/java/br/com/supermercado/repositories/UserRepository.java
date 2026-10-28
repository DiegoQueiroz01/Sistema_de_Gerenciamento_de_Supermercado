package br.com.supermercado.repositories;

import br.com.supermercado.domain.user.User;

import java.util.List;

public interface UserRepository {
    void save(User user);

    // Devolve null se o login não existir
    User findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAll();
}
