package strategy;

import model.Expense;
import model.User;
import java.math.BigDecimal;
import java.util.Map;

/** Splits according to kilometres travelled by each participant. */
public final class DistanceSplit implements SplitStrategy {
    @Override public Map<User, BigDecimal> calculate(Expense expense) {
        BigDecimal total = expense.getSplitInputs().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return ProportionalSplitSupport.calculate(expense, total, "distances");
    }
    @Override public String name() { return "Distance"; }
}
