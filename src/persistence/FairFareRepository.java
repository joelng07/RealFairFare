package persistence;

import model.Expense;
import model.Group;
import model.User;
import model.UserRole;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SQLite JDBC repository: schema setup plus storage/reconstruction of FairFare domain data. */
public final class FairFareRepository {
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "FairFareAdmin@2026";
    private final DatabaseConfig config;

    public FairFareRepository(DatabaseConfig config) {
        this.config = config;
        initialiseSchema();
    }

    /** Ensures the application always has at least one administrator. */
    public boolean ensureAdministrator() {
        try (Connection connection = connection(); PreparedStatement count = connection.prepareStatement("SELECT COUNT(*) FROM ff_users WHERE role = 'ADMIN'")) {
            try (ResultSet rows = count.executeQuery()) {
                if (rows.next() && rows.getInt(1) > 0) return false;
            }
            User admin = new User(UUID.randomUUID(), DEFAULT_ADMIN_USERNAME, "admin@fairfare.local", DEFAULT_ADMIN_PASSWORD, UserRole.ADMIN);
            try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO ff_users (id, username, email, password, role) VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, admin.getId().toString()); insert.setString(2, admin.getUsername()); insert.setString(3, admin.getEmail());
                insert.setString(4, admin.getPassword()); insert.setString(5, admin.getRole().name());
                return insert.executeUpdate() == 1;
            }
        } catch (SQLException exception) { throw failure("create administrator", exception); }
    }

    public void saveUser(User user) {
        execute("INSERT INTO ff_users (id, username, email, password, role) VALUES (?, ?, ?, ?, ?)", statement -> {
            statement.setString(1, user.getId().toString()); statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail()); statement.setString(4, user.getPassword()); statement.setString(5, user.getRole().name());
        });
    }

    public void saveGroup(Group group) {
        execute("INSERT INTO ff_groups (id, name) VALUES (?, ?)", statement -> {
            statement.setString(1, group.getId().toString()); statement.setString(2, group.getName());
        });
    }

    public void saveMembership(Group group, User user) {
        execute("INSERT OR IGNORE INTO ff_group_members (group_id, user_id) VALUES (?, ?)", statement -> {
            statement.setString(1, group.getId().toString()); statement.setString(2, user.getId().toString());
        });
    }

    public void saveExpense(Group group, Expense expense) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement expenseStatement = connection.prepareStatement("INSERT INTO ff_expenses (id, group_id, title, amount, payer_id, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                expenseStatement.setString(1, expense.getId().toString()); expenseStatement.setString(2, group.getId().toString());
                expenseStatement.setString(3, expense.getTitle()); expenseStatement.setBigDecimal(4, expense.getAmount());
                expenseStatement.setString(5, expense.getPayer().getId().toString()); expenseStatement.setString(6, expense.getCreatedAt().toString());
                expenseStatement.executeUpdate();
            }
            try (PreparedStatement participantStatement = connection.prepareStatement("INSERT INTO ff_expense_participants (expense_id, user_id, split_input, share_amount) VALUES (?, ?, ?, ?)")) {
                for (User participant : expense.getParticipants()) {
                    participantStatement.setString(1, expense.getId().toString()); participantStatement.setString(2, participant.getId().toString());
                    BigDecimal input = expense.getSplitInputs().get(participant);
                    if (input == null) participantStatement.setNull(3, java.sql.Types.DECIMAL); else participantStatement.setBigDecimal(3, input);
                    participantStatement.setBigDecimal(4, expense.getShares().get(participant)); participantStatement.addBatch();
                }
                participantStatement.executeBatch();
            }
            connection.commit();
        } catch (SQLException exception) { throw failure("save expense", exception); }
    }

    public void updateUserRole(User user) {
        execute("UPDATE ff_users SET role = ? WHERE id = ?", statement -> { statement.setString(1, user.getRole().name()); statement.setString(2, user.getId().toString()); });
    }

    public void renameGroup(Group group) {
        execute("UPDATE ff_groups SET name = ? WHERE id = ?", statement -> { statement.setString(1, group.getName()); statement.setString(2, group.getId().toString()); });
    }

    public void deleteGroup(Group group) {
        execute("DELETE FROM ff_groups WHERE id = ?", statement -> statement.setString(1, group.getId().toString()));
    }

    public void updateExpenseTitle(Expense expense) {
        execute("UPDATE ff_expenses SET title = ? WHERE id = ?", statement -> { statement.setString(1, expense.getTitle()); statement.setString(2, expense.getId().toString()); });
    }

    public void deleteExpense(Expense expense) {
        execute("DELETE FROM ff_expenses WHERE id = ?", statement -> statement.setString(1, expense.getId().toString()));
    }

    public PersistedData load() {
        try (Connection connection = connection()) {
            Map<UUID, User> users = readUsers(connection);
            Map<UUID, Group> groups = readGroups(connection);
            readMemberships(connection, users, groups);
            readExpenses(connection, users, groups);
            return new PersistedData(List.copyOf(users.values()), List.copyOf(groups.values()));
        } catch (SQLException exception) { throw failure("load saved data", exception); }
    }

    private Map<UUID, User> readUsers(Connection connection) throws SQLException {
        Map<UUID, User> users = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id, username, email, password, role FROM ff_users ORDER BY username")) {
            while (rows.next()) { User user = new User(UUID.fromString(rows.getString("id")), rows.getString("username"), rows.getString("email"), rows.getString("password"), UserRole.valueOf(rows.getString("role"))); users.put(user.getId(), user); }
        }
        return users;
    }

    private Map<UUID, Group> readGroups(Connection connection) throws SQLException {
        Map<UUID, Group> groups = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id, name FROM ff_groups ORDER BY name")) {
            while (rows.next()) { Group group = new Group(UUID.fromString(rows.getString("id")), rows.getString("name")); groups.put(group.getId(), group); }
        }
        return groups;
    }

    private void readMemberships(Connection connection, Map<UUID, User> users, Map<UUID, Group> groups) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT group_id, user_id FROM ff_group_members")) {
            while (rows.next()) { Group group = groups.get(UUID.fromString(rows.getString("group_id"))); User user = users.get(UUID.fromString(rows.getString("user_id"))); if (group != null && user != null) group.addMember(user); }
        }
    }

    private void readExpenses(Connection connection, Map<UUID, User> users, Map<UUID, Group> groups) throws SQLException {
        String sql = "SELECT id, group_id, title, amount, payer_id, created_at FROM ff_expenses ORDER BY created_at";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                UUID expenseId = UUID.fromString(rows.getString("id")); Group group = groups.get(UUID.fromString(rows.getString("group_id"))); User payer = users.get(UUID.fromString(rows.getString("payer_id")));
                if (group != null && payer != null) group.addExpense(readExpenseParticipants(connection, expenseId, rows.getString("title"), rows.getBigDecimal("amount"), payer, Instant.parse(rows.getString("created_at"))));
            }
        }
    }

    private Expense readExpenseParticipants(Connection connection, UUID expenseId, String title, BigDecimal amount, User payer, Instant createdAt) throws SQLException {
        List<User> participants = new ArrayList<>(); Map<User, BigDecimal> inputs = new LinkedHashMap<>(); Map<User, BigDecimal> shares = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT user_id, split_input, share_amount FROM ff_expense_participants WHERE expense_id = ? ORDER BY user_id")) {
            statement.setString(1, expenseId.toString()); try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    User user = findUser(connection, rows.getString("user_id"));
                    if (user == null) continue;
                    participants.add(user); BigDecimal input = rows.getBigDecimal("split_input"); if (input != null) inputs.put(user, input); shares.put(user, rows.getBigDecimal("share_amount"));
                }
            }
        }
        return Expense.restore(expenseId, title, amount, payer, participants, inputs, shares, createdAt);
    }

    private User findUser(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, username, email, password, role FROM ff_users WHERE id = ?")) {
            statement.setString(1, userId); try (ResultSet row = statement.executeQuery()) { return row.next() ? new User(UUID.fromString(row.getString("id")), row.getString("username"), row.getString("email"), row.getString("password"), UserRole.valueOf(row.getString("role"))) : null; }
        }
    }

    private void initialiseSchema() {
        String[] statements = {
                "CREATE TABLE IF NOT EXISTS ff_users (id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'USER')",
                "CREATE TABLE IF NOT EXISTS ff_groups (id TEXT PRIMARY KEY, name TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS ff_group_members (group_id TEXT NOT NULL, user_id TEXT NOT NULL, PRIMARY KEY (group_id, user_id), FOREIGN KEY (group_id) REFERENCES ff_groups(id) ON DELETE CASCADE, FOREIGN KEY (user_id) REFERENCES ff_users(id) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS ff_expenses (id TEXT PRIMARY KEY, group_id TEXT NOT NULL, title TEXT NOT NULL, amount NUMERIC NOT NULL, payer_id TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY (group_id) REFERENCES ff_groups(id) ON DELETE CASCADE, FOREIGN KEY (payer_id) REFERENCES ff_users(id))",
                "CREATE TABLE IF NOT EXISTS ff_expense_participants (expense_id TEXT NOT NULL, user_id TEXT NOT NULL, split_input NUMERIC, share_amount NUMERIC NOT NULL, PRIMARY KEY (expense_id, user_id), FOREIGN KEY (expense_id) REFERENCES ff_expenses(id) ON DELETE CASCADE, FOREIGN KEY (user_id) REFERENCES ff_users(id))"
        };
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) statement.execute(sql);
            ensureRoleColumn(connection);
        }
        catch (SQLException exception) { throw failure("initialise database schema", exception); }
    }

    private Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection(config.url());
        try (Statement statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys = ON"); }
        return connection;
    }
    private void ensureRoleColumn(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("PRAGMA table_info(ff_users)")) {
            while (rows.next()) if ("role".equalsIgnoreCase(rows.getString("name"))) return;
        }
        try (Statement statement = connection.createStatement()) { statement.execute("ALTER TABLE ff_users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'"); }
    }
    private void execute(String sql, SqlConsumer operation) { try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) { operation.accept(statement); statement.executeUpdate(); } catch (SQLException exception) { throw failure("write data", exception); } }
    private DatabaseException failure(String action, SQLException exception) { return new DatabaseException("Unable to " + action + " in FairFare's local database.", exception); }
    @FunctionalInterface private interface SqlConsumer { void accept(PreparedStatement statement) throws SQLException; }
    public record PersistedData(Collection<User> users, Collection<Group> groups) { }
}
