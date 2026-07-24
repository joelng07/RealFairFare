package service;

import model.Balance;
import model.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Produces a smaller equivalent set of settlement transfers from balances. */
public final class DebtSimplifier {
    public List<Balance> simplify(List<Balance> balances) {
        Map<User, BigDecimal> net = new HashMap<>();
        for (Balance balance : balances) {
            net.merge(balance.getDebtor(), balance.getAmount().negate(), BigDecimal::add);
            net.merge(balance.getCreditor(), balance.getAmount(), BigDecimal::add);
        }
        List<Map.Entry<User, BigDecimal>> debtors = net.entrySet().stream().filter(e -> e.getValue().signum() < 0).sorted(Map.Entry.comparingByValue()).toList();
        List<Map.Entry<User, BigDecimal>> creditors = net.entrySet().stream().filter(e -> e.getValue().signum() > 0).sorted(Map.Entry.<User, BigDecimal>comparingByValue(Comparator.reverseOrder())).toList();
        List<Balance> result = new ArrayList<>(); int debtor = 0, creditor = 0;
        List<BigDecimal> dues = debtors.stream().map(e -> e.getValue().negate()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<BigDecimal> claims = creditors.stream().map(Map.Entry::getValue).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        while (debtor < debtors.size() && creditor < creditors.size()) {
            BigDecimal amount = dues.get(debtor).min(claims.get(creditor));
            result.add(new Balance(debtors.get(debtor).getKey(), creditors.get(creditor).getKey(), amount));
            dues.set(debtor, dues.get(debtor).subtract(amount)); claims.set(creditor, claims.get(creditor).subtract(amount));
            if (dues.get(debtor).signum() == 0) debtor++; if (claims.get(creditor).signum() == 0) creditor++;
        }
        return result;
    }
}
