package app;

import model.Balance;
import model.Group;
import model.User;
import service.AuthService;
import service.BalanceManager;
import service.ExpenseManager;
import service.GroupService;
import strategy.DistanceSplit;
import strategy.EqualSplit;
import strategy.PercentageSplit;
import strategy.SplitStrategy;
import strategy.WeightSplit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Swing presentation layer. It only collects input and delegates work to services. */
final class FairFareFrame extends JFrame {
    private static final Color NAVY = new Color(29, 43, 83);
    private static final Color TEAL = new Color(20, 140, 130);
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final AuthService auth = new AuthService();
    private final GroupService groups = new GroupService();
    private final BalanceManager balances = new BalanceManager();
    private final ExpenseManager expenses = new ExpenseManager(balances);
    private User currentUser;
    private final JLabel welcome = new JLabel();
    private final JLabel receivable = new JLabel();
    private final JLabel pending = new JLabel();
    private final JLabel net = new JLabel();
    private final javax.swing.DefaultListModel<Group> groupModel = new javax.swing.DefaultListModel<>();
    private final JList<Group> groupList = new JList<>(groupModel);

    FairFareFrame() {
        super("FairFare — Shared expenses, made fair");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setLocationByPlatform(true);
        content.add(authPanel(), "AUTH");
        content.add(dashboardPanel(), "DASHBOARD");
        setContentPane(content);
        cards.show(content, "AUTH");
    }

    private JPanel authPanel() {
        JPanel page = new JPanel(new GridBagLayoutBuilder());
        page.setBackground(new Color(244, 247, 251));
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 225, 235)), BorderFactory.createEmptyBorder(28, 34, 28, 34)));
        JLabel title = new JLabel("FAIRFARE"); title.setAlignmentX(CENTER_ALIGNMENT); title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28)); title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Shared expenses, made fair."); subtitle.setAlignmentX(CENTER_ALIGNMENT); subtitle.setForeground(Color.DARK_GRAY);
        card.add(title); card.add(Box.createVerticalStrut(6)); card.add(subtitle); card.add(Box.createVerticalStrut(22));
        JTextField username = new JTextField(20); JPasswordField password = new JPasswordField(20);
        card.add(labelled("Username", username)); card.add(Box.createVerticalStrut(10)); card.add(labelled("Password", password)); card.add(Box.createVerticalStrut(16));
        JButton login = primary("Log in"); login.setAlignmentX(CENTER_ALIGNMENT); login.addActionListener(e -> login(username.getText(), new String(password.getPassword()))); card.add(login);
        card.add(Box.createVerticalStrut(10)); JButton register = secondary("Create an account"); register.setAlignmentX(CENTER_ALIGNMENT); register.addActionListener(e -> registerDialog()); card.add(register);
        page.add(card); return page;
    }

    private JPanel dashboardPanel() {
        JPanel page = new JPanel(new BorderLayout(18, 18)); page.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26)); page.setBackground(new Color(248, 250, 252));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); welcome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24)); header.add(welcome, BorderLayout.WEST);
        JButton logout = secondary("Log out"); logout.addActionListener(e -> { currentUser = null; cards.show(content, "AUTH"); }); header.add(logout, BorderLayout.EAST); page.add(header, BorderLayout.NORTH);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); groupList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        JPanel left = new JPanel(new BorderLayout(8, 8)); left.setBackground(Color.WHITE); left.setBorder(BorderFactory.createTitledBorder("Your groups")); left.add(new JScrollPane(groupList), BorderLayout.CENTER);
        JPanel groupButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6)); groupButtons.setBackground(Color.WHITE);
        JButton create = primary("+ Create group"); create.addActionListener(e -> createGroupDialog());
        JButton members = secondary("Manage members"); members.addActionListener(e -> manageMembers());
        JButton history = secondary("View history"); history.addActionListener(e -> showHistory());
        groupButtons.add(create); groupButtons.add(members); groupButtons.add(history); left.add(groupButtons, BorderLayout.SOUTH);
        JPanel right = new JPanel(); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS)); right.setBackground(new Color(248, 250, 252));
        right.add(metricPanel("Receivable", receivable, TEAL)); right.add(Box.createVerticalStrut(10)); right.add(metricPanel("Pending", pending, new Color(196, 83, 60))); right.add(Box.createVerticalStrut(10)); right.add(metricPanel("Net position", net, NAVY)); right.add(Box.createVerticalStrut(20));
        JButton addExpense = primary("Add expense"); addExpense.addActionListener(e -> expenseDialog()); right.add(addExpense); right.add(Box.createVerticalStrut(8)); JButton showBalances = new JButton("Show group balances"); showBalances.setAlignmentX(LEFT_ALIGNMENT); showBalances.addActionListener(e -> showBalances()); right.add(showBalances);
        page.add(left, BorderLayout.CENTER); page.add(right, BorderLayout.EAST); return page;
    }

    private void login(String username, String password) {
        auth.login(username.trim(), password).ifPresentOrElse(user -> { currentUser = user; refreshDashboard(); cards.show(content, "DASHBOARD"); }, () -> error("Invalid username or password."));
    }

    private void registerDialog() {
        JTextField username = new JTextField(); JTextField email = new JTextField(); JPasswordField password = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, form("Create account", "Username", username, "Email", email, "Password", password), "Create account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try { auth.register(username.getText(), email.getText(), new String(password.getPassword())); info("Account created. You can now log in."); }
            catch (IllegalArgumentException exception) { error(exception.getMessage()); }
        }
    }

    private void createGroupDialog() {
        JTextField name = new JTextField();
        if (JOptionPane.showConfirmDialog(this, form("New group", "Group name", name), "Create group", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                Group group = groups.createGroup(name.getText(), currentUser);
                refreshDashboard(); groupList.setSelectedValue(group, true);
                info("Group created. Select it and use Manage members to choose who joins this group.");
            } catch (IllegalArgumentException exception) { error(exception.getMessage()); }
        }
    }

    private void manageMembers() {
        Group group = selectedGroup();
        if (group != null) manageMembers(group);
    }

    /** Adds accounts through a visible multi-select list, never typed usernames. */
    private void manageMembers(Group group) {
        javax.swing.DefaultListModel<User> available = new javax.swing.DefaultListModel<>();
        auth.getUsers().stream().filter(user -> !group.getMembers().contains(user)).forEach(available::addElement);
        if (available.isEmpty()) { info("Every registered user is already a member of " + group.getName() + "."); return; }
        JList<User> picker = new JList<>(available);
        picker.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        picker.clearSelection();
        picker.setVisibleRowCount(7);
        JScrollPane scroll = new JScrollPane(picker);
        scroll.setPreferredSize(new Dimension(310, 150));
        JLabel current = new JLabel("Current members: " + group.getMembers());
        current.setVerticalAlignment(SwingConstants.TOP);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(current, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(this, panel, "Add members to " + group.getName(), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (picker.getSelectedValuesList().isEmpty()) { info("No members were added."); return; }
            picker.getSelectedValuesList().forEach(user -> groups.addMember(group, user));
            refreshDashboard(); groupList.setSelectedValue(group, true);
        }
    }

    private void expenseDialog() {
        Group group = selectedGroup(); if (group == null) return;
        JTextField title = new JTextField(); JTextField amount = new JTextField(); JComboBox<User> payer = new JComboBox<>(group.getMembers().toArray(User[]::new)); JComboBox<String> method = new JComboBox<>(new String[]{"Equal", "Percentage", "Weight", "Distance"});
        JPanel panel = form("New expense", "Title", title, "Amount", amount, "Paid by", payer, "Split method", method);
        if (JOptionPane.showConfirmDialog(this, panel, "Add expense", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SplitStrategy strategy = strategyFor((String) method.getSelectedItem());
            Map<User, BigDecimal> inputs = strategy instanceof EqualSplit ? Map.of() : splitInputsDialog(group.getMembers(), strategy.name());
            if (inputs == null) return;
            expenses.addExpense(group, title.getText(), new BigDecimal(amount.getText()), (User) payer.getSelectedItem(), group.getMembers(), strategy, inputs);
            refreshDashboard(); info("Expense saved using the " + strategy.name() + " split.");
        } catch (NumberFormatException exception) { error("Amount must be a valid number."); }
        catch (IllegalArgumentException exception) { error(exception.getMessage()); }
    }

    private Map<User, BigDecimal> splitInputsDialog(List<User> members, String method) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8)); Map<User, JTextField> fields = new LinkedHashMap<>(); String unit = method.equals("Percentage") ? "%" : method.equals("Distance") ? "km" : "weight";
        for (User user : members) { JTextField field = new JTextField(); fields.put(user, field); panel.add(new JLabel(user.getUsername() + " (" + unit + ")")); panel.add(field); }
        if (JOptionPane.showConfirmDialog(this, panel, method + " split", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        Map<User, BigDecimal> result = new LinkedHashMap<>(); fields.forEach((user, field) -> result.put(user, new BigDecimal(field.getText()))); return result;
    }

    private void showBalances() {
        Group group = selectedGroup(); if (group == null) return;
        StringBuilder text = new StringBuilder(group.getName()).append(" balances\n\n");
        if (group.getBalances().isEmpty()) text.append("Everyone is settled up.");
        for (Balance balance : group.getBalances()) text.append(balance.getDebtor()).append(" owes ").append(balance.getCreditor()).append("  ₹").append(money(balance.getAmount())).append('\n');
        JOptionPane.showMessageDialog(this, text.toString(), "Balances", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHistory() {
        Group group = selectedGroup(); if (group == null) return;
        StringBuilder text = new StringBuilder(group.getName()).append(" expense history\n\n");
        if (group.getExpenses().isEmpty()) text.append("No expenses yet.");
        group.getExpenses().forEach(expense -> text.append(expense.getTitle()).append(" — ₹").append(money(expense.getAmount())).append(", paid by ").append(expense.getPayer()).append('\n'));
        JOptionPane.showMessageDialog(this, text.toString(), "Expense history", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshDashboard() {
        welcome.setText("Welcome back, " + currentUser.getUsername()); groupModel.clear(); groups.groupsFor(currentUser).forEach(groupModel::addElement);
        receivable.setText("₹" + money(balances.receivableFor(currentUser))); pending.setText("₹" + money(balances.pendingFor(currentUser))); BigDecimal amount = balances.netFor(currentUser); net.setText((amount.signum() >= 0 ? "+" : "") + "₹" + money(amount));
    }
    private Group selectedGroup() { Group group = groupList.getSelectedValue(); if (group == null) error("Select a group first."); return group; }
    private SplitStrategy strategyFor(String name) { return switch (name) { case "Percentage" -> new PercentageSplit(); case "Weight" -> new WeightSplit(); case "Distance" -> new DistanceSplit(); default -> new EqualSplit(); }; }
    private JPanel labelled(String label, java.awt.Component field) { JPanel panel = new JPanel(new BorderLayout(4, 4)); panel.setOpaque(false); panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(field, BorderLayout.CENTER); panel.setMaximumSize(new Dimension(300, 60)); return panel; }
    private JPanel metricPanel(String label, JLabel value, Color color) { JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setAlignmentX(LEFT_ALIGNMENT); panel.setBackground(Color.WHITE); panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 225, 235)), BorderFactory.createEmptyBorder(14, 18, 14, 70))); JLabel heading = new JLabel(label); heading.setForeground(Color.DARK_GRAY); value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22)); value.setForeground(color); panel.add(heading); panel.add(Box.createVerticalStrut(5)); panel.add(value); return panel; }
    private JButton primary(String text) {
        JButton button = new JButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setBackground(NAVY); button.setForeground(Color.WHITE); button.setOpaque(true); button.setContentAreaFilled(true);
        button.setFocusPainted(false); button.setRolloverEnabled(true); button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(14, 24, 55), 1), BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        button.setPreferredSize(new Dimension(180, 44)); button.setMaximumSize(new Dimension(300, 44));
        button.setAlignmentX(LEFT_ALIGNMENT);
        return button;
    }
    private JButton secondary(String text) {
        JButton button = new JButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setBackground(new Color(235, 240, 248)); button.setForeground(new Color(12, 28, 66)); button.setOpaque(true); button.setContentAreaFilled(true);
        button.setFocusPainted(false); button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(130, 148, 180), 1), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setPreferredSize(new Dimension(150, 44));
        return button;
    }
    private JPanel form(String title, Object... values) { JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10)); for (int i = 0; i < values.length; i += 2) { panel.add(new JLabel((String) values[i])); panel.add((java.awt.Component) values[i + 1]); } return panel; }
    private void info(String message) { JOptionPane.showMessageDialog(this, message, "FairFare", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "FairFare", JOptionPane.ERROR_MESSAGE); }
    private String money(BigDecimal value) { return value.setScale(2).toPlainString(); }
}

/** Lightweight layout used to center the authentication card. */
final class GridBagLayoutBuilder extends java.awt.GridBagLayout { }
