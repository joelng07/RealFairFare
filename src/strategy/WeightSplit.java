package strategy;

import model.Expense;
import model.User;
import java.math.BigDecimal;
import java.util.Map;

/** Splits according to positive relative weights. */
public final class WeightSplit implements SplitStrategy {
    @Override public Map<User, BigDecimal> calculate(Expense expense) {
        BigDecimal total = expense.getSplitInputs().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return ProportionalSplitSupport.calculate(expense, total, "weights");
    }
    @Override public String name() { return "Weight"; }
}
