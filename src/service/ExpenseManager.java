package service;

import model.Expense;
import model.Group;
import model.User;
import strategy.SplitStrategy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates, calculates, records, and posts a new expense. */
public final class ExpenseManager {
    private final BalanceManager balanceManager;
    public ExpenseManager(BalanceManager balanceManager) { this.balanceManager = Objects.requireNonNull(balanceManager); }
    public Expense addExpense(Group group, String title, BigDecimal amount, User payer, List<User> participants,
                              SplitStrategy strategy, Map<User, BigDecimal> splitInputs) {
        Objects.requireNonNull(group); Objects.requireNonNull(strategy);
        if (!group.getMembers().contains(payer) || !group.getMembers().containsAll(participants)) {
            throw new IllegalArgumentException("Payer and participants must be group members");
        }
        Expense request = Expense.request(title, amount, payer, participants, splitInputs);
        Expense expense = request.withShares(strategy.calculate(request));
        group.addExpense(expense);
        balanceManager.applyExpense(group, expense);
        return expense;
    }
}
