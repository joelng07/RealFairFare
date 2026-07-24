package service;

import model.Balance;
import model.Expense;
import model.Group;
import model.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maintains derived debtor-creditor relationships independently of users. */
public final class BalanceManager {
    private final Map<Group, Map<User, Map<User, BigDecimal>>> ledger = new HashMap<>();

    public void applyExpense(Group group, Expense expense) {
        for (Map.Entry<User, BigDecimal> share : expense.getShares().entrySet()) {
            if (!share.getKey().equals(expense.getPayer())) addDebt(group, share.getKey(), expense.getPayer(), share.getValue());
        }
    }

    public List<Balance> balancesFor(Group group) { return group.getBalances(); }
    public BigDecimal receivableFor(User user) { return total(user, false); }
    public BigDecimal pendingFor(User user) { return total(user, true); }
    public BigDecimal netFor(User user) { return receivableFor(user).subtract(pendingFor(user)); }

    private void addDebt(Group group, User debtor, User creditor, BigDecimal amount) {
        Map<User, Map<User, BigDecimal>> groupLedger = ledger.computeIfAbsent(group, ignored -> new HashMap<>());
        BigDecimal reverse = groupLedger.getOrDefault(creditor, Map.of()).getOrDefault(debtor, BigDecimal.ZERO);
        if (reverse.compareTo(amount) >= 0) {
            put(groupLedger, creditor, debtor, reverse.subtract(amount));
        } else {
            put(groupLedger, creditor, debtor, BigDecimal.ZERO);
            put(groupLedger, debtor, creditor, amount.subtract(reverse));
        }
        refreshGroupBalances(group, groupLedger);
    }

    private void put(Map<User, Map<User, BigDecimal>> groupLedger, User debtor, User creditor, BigDecimal amount) {
        Map<User, BigDecimal> debts = groupLedger.computeIfAbsent(debtor, ignored -> new HashMap<>());
        if (amount.signum() == 0) debts.remove(creditor); else debts.put(creditor, amount);
    }

    private void refreshGroupBalances(Group group, Map<User, Map<User, BigDecimal>> groupLedger) {
        List<Balance> balances = new ArrayList<>();
        groupLedger.forEach((debtor, debts) -> debts.forEach((creditor, amount) -> balances.add(new Balance(debtor, creditor, amount))));
        group.replaceBalances(balances);
    }

    private BigDecimal total(User user, boolean pending) {
        return ledger.values().stream().flatMap(group -> group.entrySet().stream())
                .flatMap(entry -> entry.getValue().entrySet().stream().map(debt -> new Balance(entry.getKey(), debt.getKey(), debt.getValue())))
                .filter(balance -> pending ? balance.getDebtor().equals(user) : balance.getCreditor().equals(user))
                .map(Balance::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
