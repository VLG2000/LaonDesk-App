package loandesk.model;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * One physical item lent to one borrower.
 */
public final class Loan {

    /**
     * Display order required by the spec: loan date newest → oldest, and when two
     * loans share a date, the higher loan ID first.
     */
    public static final Comparator<Loan> NEWEST_FIRST =
            Comparator.comparing(Loan::getLoanDate).reversed()
                    .thenComparing(Comparator.comparingInt(Loan::getLoanId).reversed());

    /** Auto-generated positive ID; 0 means "not saved yet". */
    private final int loanId;

    /** ID of the user who owns this loan (stable user ID, not a display name). */
    private final int ownerId;

    private final String itemCode;      // always stored in UPPERCASE, e.g. DRILL-01
    private final String itemName;      // e.g. "Cordless drill"
    private final String borrowerName;  // free text; borrowers do not have accounts
    private final Category category;
    private final LocalDate loanDate;   // set automatically to "today" when created
    private final LocalDate dueDate;    // chosen by the user, >= loanDate
    private final LocalDate returnDate; // null until "Mark returned" is clicked
    private final String note;          // optional, empty string when absent

    /**
     * Creates a loan object. Validation is done by the service layer before
     * this constructor is called, so this class just stores the values.
     */
    public Loan(int loanId, int ownerId, String itemCode, String itemName, String borrowerName,
                Category category, LocalDate loanDate, LocalDate dueDate, LocalDate returnDate,
                String note) {
        this.loanId = loanId;
        this.ownerId = ownerId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.borrowerName = borrowerName;
        this.category = category;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        // Treat null as "no note" so other code never has to null-check the note.
        this.note = note == null ? "" : note;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * @return the return date, or {@code null} if the item has not been returned
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getNote() {
        return note;
    }

    /**
     * @return true once the item has been marked as returned
     */
    public boolean isReturned() {
        return returnDate != null;
    }

    /**
     * Convenience method that delegates to {@link LoanStatus#calculate(Loan, LocalDate)}.
     *
     * @param today the current date
     * @return On loan, Overdue or Returned
     */
    public LoanStatus getStatus(LocalDate today) {
        return LoanStatus.calculate(this, today);
    }

    @Override
    public String toString() {
        return "Loan #" + loanId + " " + itemCode + " (" + itemName + ")";
    }
}
