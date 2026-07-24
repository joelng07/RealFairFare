package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProportionalSplitSupport {
    private ProportionalSplitSupport() { }

    static Map<User, BigDecimal> calculate(Expense expense, BigDecimal requiredTotal, String label) {
        Map<User, BigDecimal> inputs = expense.getSplitInputs();
        if (!inputs.keySet().equals(new java.util.LinkedHashSet<>(expense.getParticipants()))) {
            throw new IllegalArgumentException(label + " must be supplied for every participant");
        }
        BigDecimal total = inputs.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(requiredTotal) != 0) {
            throw new IllegalArgumentException(label + " total must equal " + requiredTotal.stripTrailingZeros().toPlainString());
        }
        Map<User, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (User participant : expense.getParticipants()) {
            BigDecimal share = expense.getAmount().multiply(inputs.get(participant)).divide(total, 2, RoundingMode.DOWN);
            shares.put(participant, share);
            allocated = allocated.add(share);
        }
        BigDecimal remainder = expense.getAmount().subtract(allocated);
        for (User participant : expense.getParticipants()) {
            if (remainder.signum() == 0) break;
            shares.computeIfPresent(participant, (user, value) -> value.add(new BigDecimal("0.01")));
            remainder = remainder.subtract(new BigDecimal("0.01"));
        }
        return shares;
    }
}
