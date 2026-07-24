package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Owns the members, expense history, and outstanding balances for one sharing context. */
public final class Group {
    private final UUID id;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final List<Balance> balances = new ArrayList<>();

    public Group(String name) {
        this(UUID.randomUUID(), name);
    }

    public Group(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("group name must not be blank");
        }
        this.name = name.trim();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public List<User> getMembers() { return Collections.unmodifiableList(members); }
    public List<Expense> getExpenses() { return Collections.unmodifiableList(expenses); }
    public List<Balance> getBalances() { return Collections.unmodifiableList(balances); }

    public void addMember(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (!members.contains(user)) {
            members.add(user);
            user.joinGroup(this);
        }
    }

    public void removeMember(User user) {
        Objects.requireNonNull(user, "user must not be null");
        members.remove(user);
        user.leaveGroup(this);
    }

    public void addExpense(Expense expense) {
        expenses.add(Objects.requireNonNull(expense, "expense must not be null"));
    }

    public void addBalance(Balance balance) {
        balances.add(Objects.requireNonNull(balance, "balance must not be null"));
    }

    /** Replaces the derived balance snapshot; only BalanceManager should call this. */
    public void replaceBalances(List<Balance> updatedBalances) {
        balances.clear();
        balances.addAll(Objects.requireNonNull(updatedBalances, "updatedBalances must not be null"));
    }

    @Override public boolean equals(Object object) {
        return object instanceof Group group && id.equals(group.id);
    }

    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return name; }
}
