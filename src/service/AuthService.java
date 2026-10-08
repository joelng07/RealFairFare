package service;

import model.User;
import persistence.FairFareRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Manages in-memory registration and credential validation. */
public final class AuthService {
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();
    private final FairFareRepository repository;

    public AuthService(FairFareRepository repository, Collection<User> existingUsers) {
        this.repository = java.util.Objects.requireNonNull(repository, "repository must not be null");
        existingUsers.forEach(user -> usersByUsername.put(user.getUsername(), user));
    }

    public User register(String username, String email, String password) {
        if (usersByUsername.containsKey(username)) throw new IllegalArgumentException("Username is already taken");
        if (usersByUsername.values().stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(email))) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = new User(username, email, password);
        repository.saveUser(user);
        usersByUsername.put(username, user);
        return user;
    }

    public Optional<User> login(String username, String password) {
        User user = usersByUsername.get(username);
        return user != null && user.getPassword().equals(password) ? Optional.of(user) : Optional.empty();
    }

    public Optional<User> findByUsername(String username) { return Optional.ofNullable(usersByUsername.get(username)); }

    public Collection<User> getUsers() { return java.util.List.copyOf(usersByUsername.values()); }
}
