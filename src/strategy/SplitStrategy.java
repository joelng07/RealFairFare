package strategy;

import model.Expense;
import model.User;

import java.math.BigDecimal;
import java.util.Map;

/** Calculates each participant's share of an expense request. */
public interface SplitStrategy {
    Map<User, BigDecimal> calculate(Expense expense);
    String name();
}
