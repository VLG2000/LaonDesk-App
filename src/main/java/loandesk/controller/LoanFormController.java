package loandesk.controller;

import javafx.application.Platform;
import javafx.stage.Window;
import loandesk.model.Loan;
import loandesk.service.LoanService;
import loandesk.view.LoanFormView;

import java.time.LocalDate;
import java.util.Optional;

/**
 * CONTROLLER for the New loan and Edit loan dialogs.
 */
public class LoanFormController {

    private final LoanService loanService;
    private final int ownerId;

    /**
     * @param loanService loan business logic
     * @param ownerId     the logged-in user's ID (taken from the session by the caller)
     */
    public LoanFormController(LoanService loanService, int ownerId) {
        this.loanService = loanService;
        this.ownerId = ownerId;
    }

    /**
     * Shows an empty form and creates a new loan when Save is clicked.
     *
     * @param owner parent window
     * @return the created loan, or empty if cancelled
     */
    public Optional<Loan> showCreateDialog(Window owner) {
        LoanFormView view = new LoanFormView();
        LocalDate today = loanService.today();
        view.setLoanIdText("(assigned automatically when saved)");
        view.setLoanDateText(today + " (today)");
        view.restrictDueDateFrom(today);
        Platform.runLater(() -> view.getItemCodeField().requestFocus());

        return FormDialog.show(owner, "LoanDesk - New loan", "Record an item you are lending today",
                view.getRoot(), view.getErrorLabel(),
                () -> loanService.createLoan(ownerId, view.readInput()));
    }

    /**
     * Shows the form pre-filled with an existing loan and saves the changes.
     *
     * @param owner parent window
     * @param loan  the (unreturned) loan to edit
     * @return the updated loan, or empty if cancelled
     */
    public Optional<Loan> showEditDialog(Window owner, Loan loan) {
        LoanFormView view = new LoanFormView();
        view.fill(loan);
        view.restrictDueDateFrom(loan.getLoanDate());

        return FormDialog.show(owner, "LoanDesk - Edit loan", "Edit loan #" + loan.getLoanId(),
                view.getRoot(), view.getErrorLabel(),
                () -> loanService.updateLoan(ownerId, loan.getLoanId(), view.readInput()));
    }
}
