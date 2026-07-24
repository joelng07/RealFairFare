package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable record of an expense and the shares calculated for its participants. */
public final class Expense {
    private final UUID id;
    private final String title;
    private final BigDecimal amount;
    private final User payer;
    private final List<User> participants;
    private final Map<User, BigDecimal> splitInputs;
    private final Map<User, BigDecimal> shares;
    private final Instant createdAt;

    public Expense(String title, BigDecimal amount, User payer, List<User> participants,
                   Map<User, BigDecimal> shares) {
        this(UUID.randomUUID(), title, amount, payer, participants, Map.of(), shares, Instant.now());
    }

    public Expense(UUID id, String title, BigDecimal amount, User payer, List<User> participants,
                   Map<User, BigDecimal> shares, Instant createdAt) {
        this(id, title, amount, payer, participants, Map.of(), shares, createdAt);
    }

    private Expense(UUID id, String title, BigDecimal amount, User payer, List<User> participants,
                    Map<User, BigDecimal> splitInputs, Map<User, BigDecimal> shares, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        this.title = title.trim();
        this.amount = money(amount, "amount");
        this.payer = Objects.requireNonNull(payer, "payer must not be null");
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        this.participants = List.copyOf(participants);
        if (new java.util.LinkedHashSet<>(this.participants).size() != this.participants.size()) {
            throw new IllegalArgumentException("participants must not contain duplicates");
        }
        if (!this.participants.contains(payer)) throw new IllegalArgumentException("payer must be a participant");
        this.splitInputs = immutableInputs(splitInputs);
        this.shares = shares.isEmpty() ? Map.of() : immutableShares(shares);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /** Creates an uncalculated expense request for a split strategy. */
    public static Expense request(String title, BigDecimal amount, User payer, List<User> participants,
                                  Map<User, BigDecimal> splitInputs) {
        return new Expense(UUID.randomUUID(), title, amount, payer, participants, splitInputs, Map.of(), Instant.now());
    }

    /** Returns the completed form of this request after a strategy calculates its shares. */
    public Expense withShares(Map<User, BigDecimal> calculatedShares) {
        return new Expense(id, title, amount, payer, participants, splitInputs, calculatedShares, createdAt);
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public User getPayer() { return payer; }
    public List<User> getParticipants() { return participants; }
    public Map<User, BigDecimal> getSplitInputs() { return splitInputs; }
    public Map<User, BigDecimal> getShares() { return shares; }
    public Instant getCreatedAt() { return createdAt; }

    private Map<User, BigDecimal> immutableShares(Map<User, BigDecimal> input) {
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("shares must not be empty");
        Map<User, BigDecimal> copy = new LinkedHashMap<>();
        input.forEach((user, share) -> copy.put(Objects.requireNonNull(user, "share user must not be null"), money(share, "share")));
        if (!copy.keySet().equals(new java.util.LinkedHashSet<>(participants))) {
            throw new IllegalArgumentException("shares must be supplied for exactly the participants");
        }
        BigDecimal total = copy.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(amount) != 0) throw new IllegalArgumentException("shares must add up to the expense amount");
        return Collections.unmodifiableMap(copy);
    }

    private Map<User, BigDecimal> immutableInputs(Map<User, BigDecimal> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<User, BigDecimal> copy = new LinkedHashMap<>();
        input.forEach((user, value) -> {
            Objects.requireNonNull(user, "split input user must not be null");
            Objects.requireNonNull(value, "split input must not be null");
            if (value.signum() <= 0) throw new IllegalArgumentException("split inputs must be greater than zero");
            copy.put(user, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    private static BigDecimal money(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.signum() <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
