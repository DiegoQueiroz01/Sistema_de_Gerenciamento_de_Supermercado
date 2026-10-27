package br.com.supermercado.infrastructure.memory;

import br.com.supermercado.domain.user.User;
import br.com.supermercado.repositories.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mapa com o login como chave: busca direta e não deixa repetir login
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new LinkedHashMap<>();

    @Override
    public void save(User user) {
        users.put(user.username(), user);
    }

    @Override
    public User findByUsername(String username) {
        return users.get(User.normalizeUsername(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.containsKey(User.normalizeUsername(username));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}
