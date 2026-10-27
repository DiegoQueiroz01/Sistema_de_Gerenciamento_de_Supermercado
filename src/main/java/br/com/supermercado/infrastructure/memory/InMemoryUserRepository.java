package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.user.User;
import br.com.supermercado.repositories.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mapa por login: busca direta no login e impede logins repetidos. */
public final class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();

    @Override
    public void save(User user) {
        usersByUsername.put(user.username(), user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(User.normalizeUsername(username)));
    }

    @Override
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(User.normalizeUsername(username));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(usersByUsername.values());
    }
}
