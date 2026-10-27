package br.com.supermercado.repositories;

import br.com.supermercado.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAll();
}
