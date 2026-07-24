package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** An outstanding amount that one user owes another user. */
public final class Balance {
    private final User debtor;
    private final User creditor;
    private BigDecimal amount;

    public Balance(User debtor, User creditor, BigDecimal amount) {
        this.debtor = Objects.requireNonNull(debtor, "debtor must not be null");
        this.creditor = Objects.requireNonNull(creditor, "creditor must not be null");
        if (debtor.equals(creditor)) {
            throw new IllegalArgumentException("debtor and creditor must be different users");
        }
        setAmount(amount);
    }

    public User getDebtor() { return debtor; }
    public User getCreditor() { return creditor; }
    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
}
