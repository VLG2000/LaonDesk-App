package loandesk.model;

import java.time.LocalDate;

/**
 * The status of a loan. The status is NOT stored in the database; it is always
 * calculated from the dates, so it can never get out of date. The GUI and the
 * export both use {@link #calculate(Loan, LocalDate)}, which guarantees they follow
 * exactly the same rules.
 */
public enum LoanStatus {
    ON_LOAN("On loan"),
    OVERDUE("Overdue"),
    RETURNED("Returned");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Works out a loan's status on a given day.
     * @param loan  the loan to check
     * @param today the date to evaluate against (passed in so tests can use fixed dates)
     * @return the calculated status
     */
    public static LoanStatus calculate(Loan loan, LocalDate today) {
        if (loan.getReturnDate() != null) {
            return RETURNED;
        }
        if (today.isAfter(loan.getDueDate())) {
            return OVERDUE;
        }
        return ON_LOAN;
    }
}
