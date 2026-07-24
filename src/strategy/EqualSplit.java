package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Divides an amount evenly, assigning any rounding remainder deterministically. */
public final class EqualSplit implements SplitStrategy {
    @Override public Map<User, BigDecimal> calculate(Expense expense) {
        int count = expense.getParticipants().size();
        BigDecimal base = expense.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal remainder = expense.getAmount().subtract(base.multiply(BigDecimal.valueOf(count)));
        Map<User, BigDecimal> shares = new LinkedHashMap<>();
        for (User participant : expense.getParticipants()) {
            BigDecimal extra = remainder.signum() > 0 ? new BigDecimal("0.01") : BigDecimal.ZERO;
            shares.put(participant, base.add(extra));
            remainder = remainder.subtract(extra);
        }
        return shares;
    }
    @Override public String name() { return "Equal"; }
}
