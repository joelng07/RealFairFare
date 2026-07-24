package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Represents an application account and its group memberships. */
public final class User {
    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final List<Group> groups = new ArrayList<>();

    public User(String username, String email, String password) {
        this(UUID.randomUUID(), username, email, password);
    }

    public User(UUID id, String username, String email, String password) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = requireText(username, "username");
        this.email = requireText(email, "email");
        this.password = requireText(password, "password");
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public List<Group> getGroups() { return Collections.unmodifiableList(groups); }

    public void joinGroup(Group group) {
        Objects.requireNonNull(group, "group must not be null");
        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    public void leaveGroup(Group group) {
        groups.remove(Objects.requireNonNull(group, "group must not be null"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    @Override public boolean equals(Object object) {
        return object instanceof User user && id.equals(user.id);
    }

    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return username; }
}
