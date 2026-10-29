package loandesk.view;

import loandesk.model.Loan;
import loandesk.model.LoanStatus;

/**
 * One row of the loans table: a loan plus its status as calculated at the last refresh.
 *
 * @param loan   the loan being shown
 * @param status its status on the day of the last refresh
 */
public record LoanRow(Loan loan, LoanStatus status) {

    /** Shown when a value is absent. */
    public static final String DASH = "-";

    public String loanIdText() {
        return String.valueOf(loan.getLoanId());
    }

    public String returnDateText() {
        return loan.getReturnDate() == null ? DASH : loan.getReturnDate().toString();
    }

    /** The note on a single line (line breaks become spaces) for the table cell. */
    public String noteSummary() {
        String note = loan.getNote();
        return note.isBlank() ? DASH : note.replaceAll("\\s*\\R\\s*", " ");
    }

    /**
     * Full multi-line description shown in the details panel below the table
     * (so long notes can be read in full).
     */
    public String detailText() {
        return "Loan ID:      " + loan.getLoanId() + "\n"
                + "Item:         " + loan.getItemCode() + " - " + loan.getItemName() + "\n"
                + "Borrower:     " + loan.getBorrowerName() + "\n"
                + "Category:     " + loan.getCategory().getLabel() + "\n"
                + "Loan date:    " + loan.getLoanDate() + "\n"
                + "Due date:     " + loan.getDueDate() + "\n"
                + "Return date:  " + returnDateText() + "\n"
                + "Status:       " + status.getLabel() + "\n"
                + "Note:         " + (loan.getNote().isBlank() ? DASH : loan.getNote());
    }
}
