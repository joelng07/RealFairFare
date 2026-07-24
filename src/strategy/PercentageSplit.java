package strategy;

import model.Expense;
import model.User;
import java.math.BigDecimal;
import java.util.Map;

/** Splits according to participant percentages, which must total 100. */
public final class PercentageSplit implements SplitStrategy {
    @Override public Map<User, BigDecimal> calculate(Expense expense) {
        return ProportionalSplitSupport.calculate(expense, new BigDecimal("100"), "percentages");
    }
    @Override public String name() { return "Percentage"; }
}
