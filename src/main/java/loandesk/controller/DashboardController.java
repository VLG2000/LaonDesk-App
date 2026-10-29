package loandesk.controller;

import javafx.stage.FileChooser;
import loandesk.dao.DataAccessException;
import loandesk.model.Loan;
import loandesk.model.User;
import loandesk.service.AuthService;
import loandesk.service.ExportService;
import loandesk.service.LoanService;
import loandesk.service.Session;
import loandesk.validation.ValidationException;
import loandesk.view.Alerts;
import loandesk.view.DashboardView;
import loandesk.view.LoanRow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CONTROLLER for the dashboard. It reacts to every button on the main screen, calls the
 * services (model) and then updates the view with the saved data.
 */
public class DashboardController {

    private final DashboardView view;
    private final SceneNavigator navigator;
    private final Session session;
    private final AuthService authService;
    private final LoanService loanService;
    private final ExportService exportService;

    public DashboardController(DashboardView view, SceneNavigator navigator, Session session,
                               AuthService authService, LoanService loanService,
                               ExportService exportService) {
        this.view = view;
        this.navigator = navigator;
        this.session = session;
        this.authService = authService;
        this.loanService = loanService;
        this.exportService = exportService;

        wireEvents();
        updateWelcomeMessage();
        // Statuses are recalculated on login, because the dashboard is created at login.
        refreshLoans();
        updateButtonStates();
    }

    /** Connects every button and the table selection to a handler method. */
    private void wireEvents() {
        view.getNewLoanButton().setOnAction(event -> handleNewLoan());
        view.getEditLoanButton().setOnAction(event -> handleEditLoan());
        view.getMarkReturnedButton().setOnAction(event -> handleMarkReturned());
        view.getDeleteLoanButton().setOnAction(event -> handleDeleteLoan());
        view.getRefreshButton().setOnAction(event -> {
            if (refreshLoans()) {
                view.getFeedbackLabel().showSuccess("Loans refreshed and statuses updated for " + loanService.today() + ".");
            }
        });
        view.getExportButton().setOnAction(event -> handleExport());
        view.getEditProfileButton().setOnAction(event -> handleEditProfile());
        view.getChangePasswordButton().setOnAction(event -> handleChangePassword());
        view.getLogoutButton().setOnAction(event -> handleLogout());

        // Observer: whenever the selected row changes, update the buttons and details panel.
        view.getLoanTable().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldRow, newRow) -> {
                    updateButtonStates();
                    view.getDetailArea().setText(newRow == null
                            ? "Select a loan to see all of its details." : newRow.detailText());
                });
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /** The logged-in user's ID. Always taken from the session, never from the GUI. */
    private int currentUserId() {
        return session.getCurrentUser().getUserId();
    }

    private void updateWelcomeMessage() {
        view.getWelcomeLabel().setText("Welcome, " + session.getCurrentUser().getFullName());
    }

    /**
     * Reloads the user's loans from the database and recalculates each status for today.
     *
     * @return true if successful, false if loading failed (an error has been shown)
     */
    private boolean refreshLoans() {
        try {
            LocalDate today = loanService.today();
            List<LoanRow> rows = loanService.getLoans(currentUserId()).stream()
                    .map(loan -> new LoanRow(loan, loan.getStatus(today)))
                    .toList();
            view.getLoanTable().getItems().setAll(rows);
            return true;
        } catch (DataAccessException e) {
            Alerts.showError("Could not load your loans", e.getMessage());
            return false;
        }
    }

    /** After a refresh, re-select a loan by ID so the user keeps their place. */
    private void selectLoan(int loanId) {
        for (LoanRow row : view.getLoanTable().getItems()) {
            if (row.loan().getLoanId() == loanId) {
                view.getLoanTable().getSelectionModel().select(row);
                view.getLoanTable().scrollTo(row);
                return;
            }
        }
    }

    /** @return the selected loan, or null if no row is selected */
    private Loan selectedLoan() {
        LoanRow row = view.getLoanTable().getSelectionModel().getSelectedItem();
        return row == null ? null : row.loan();
    }

    /**
     * Enables/disables buttons depending on the selection:
     * Edit and Mark returned need an unreturned loan; Delete needs any loan.
     */
    private void updateButtonStates() {
        Loan selected = selectedLoan();
        boolean nothingSelected = selected == null;
        boolean returned = selected != null && selected.isReturned();
        view.getEditLoanButton().setDisable(nothingSelected || returned);
        view.getMarkReturnedButton().setDisable(nothingSelected || returned);
        view.getDeleteLoanButton().setDisable(nothingSelected);
    }

    /**
     * Returns the selected loan, or explains why the action cannot proceed.
     * (The buttons are normally disabled, so this is a second line of defence.)
     */
    private Loan requireSelection(String action) {
        Loan selected = selectedLoan();
        if (selected == null) {
            Alerts.showInfo("No loan selected", "Please select a loan in the table before choosing \"" + action + "\".");
        }
        return selected;
    }

    // =====================================================================================
    // Loan actions
    // =====================================================================================

    private void handleNewLoan() {
        Optional<Loan> created = new LoanFormController(loanService, currentUserId())
                .showCreateDialog(navigator.getStage());
        created.ifPresent(loan -> {
            refreshLoans();
            selectLoan(loan.getLoanId());
            view.getFeedbackLabel().showSuccess("Loan #" + loan.getLoanId() + " for " + loan.getItemCode()
                    + " saved.");
        });
    }

    private void handleEditLoan() {
        Loan selected = requireSelection("Edit loan");
        if (selected == null) {
            return;
        }
        if (selected.isReturned()) {
            Alerts.showInfo("Loan already returned", "Returned loans are kept as history and cannot be edited.");
            return;
        }
        Optional<Loan> updated = new LoanFormController(loanService, currentUserId())
                .showEditDialog(navigator.getStage(), selected);
        updated.ifPresent(loan -> {
            refreshLoans();
            selectLoan(loan.getLoanId());
            view.getFeedbackLabel().showSuccess("Loan #" + loan.getLoanId() + " updated.");
        });
    }

    private void handleMarkReturned() {
        Loan selected = requireSelection("Mark returned");
        if (selected == null) {
            return;
        }
        if (selected.isReturned()) {
            Alerts.showInfo("Loan already returned", "This loan was returned on " + selected.getReturnDate() + ".");
            return;
        }
        boolean confirmed = Alerts.confirm("LoanDesk - Mark returned",
                "Mark " + selected.getItemCode() + " (" + selected.getItemName() + ") as returned?",
                "Today's date (" + loanService.today() + ") will be recorded as the return date. "
                        + "This cannot be undone.");
        if (!confirmed) {
            return;
        }
        try {
            loanService.markReturned(currentUserId(), selected.getLoanId());
            refreshLoans();
            selectLoan(selected.getLoanId());
            view.getFeedbackLabel().showSuccess(selected.getItemCode() + " marked as returned.");
        } catch (ValidationException e) {
            Alerts.showError("Could not mark as returned", e.getMessage());
            refreshLoans();
        } catch (DataAccessException e) {
            Alerts.showError("Could not mark as returned", e.getMessage() + "\nNothing was changed.");
        }
    }

    private void handleDeleteLoan() {
        Loan selected = requireSelection("Delete loan");
        if (selected == null) {
            return;
        }
        boolean confirmed = Alerts.confirm("LoanDesk - Delete loan",
                "Delete loan #" + selected.getLoanId() + " (" + selected.getItemCode() + ")?",
                "The loan record will be permanently removed.");
        if (!confirmed) {
            return;
        }
        try {
            loanService.deleteLoan(currentUserId(), selected.getLoanId());
            refreshLoans();
            view.getFeedbackLabel().showSuccess("Loan #" + selected.getLoanId() + " deleted.");
        } catch (ValidationException e) {
            Alerts.showError("Could not delete the loan", e.getMessage());
            refreshLoans();
        } catch (DataAccessException e) {
            Alerts.showError("Could not delete the loan", e.getMessage() + "\nNothing was changed.");
        }
    }

    // =====================================================================================
    // Export
    // =====================================================================================

    private void handleExport() {
        // Spec: recalculate statuses before exporting. If loading fails, do not export.
        if (!refreshLoans()) {
            return;
        }
        User user = session.getCurrentUser();
        LocalDate today = loanService.today();

        // Let the user choose name and folder with the standard JavaFX file chooser.
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export all loans");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        chooser.setInitialFileName("loandesk-" + user.getUsername() + "-" + today + ".txt");
        // The native save dialog itself asks "Replace existing file?" if the name exists.
        File chosen = chooser.showSaveDialog(navigator.getStage());
        if (chosen == null) {
            return; // cancelled: nothing changes
        }

        Path file = chosen.toPath();
        if (!file.getFileName().toString().toLowerCase().endsWith(".txt")) {
            // Add the extension. The chooser did not see this name, so ask before overwriting.
            file = file.resolveSibling(file.getFileName() + ".txt");
            if (Files.exists(file) && !Alerts.confirm("LoanDesk - Export",
                    "Replace existing file?", file + " already exists. Do you want to overwrite it?")) {
                return;
            }
        }

        try {
            // Export uses the loans from the database (all of them, not just what is selected).
            List<Loan> loans = loanService.getLoans(user.getUserId());
            String report = exportService.buildReport(user, loans, today);
            exportService.writeReport(file, report);
            // Only reached after the file was written successfully.
            view.getFeedbackLabel().showSuccess("Exported " + loans.size() + " loan(s) to " + file);
            Alerts.showInfo("Export complete", "Exported " + loans.size() + " loan(s) to:\n" + file);
        } catch (IOException e) {
            Alerts.showError("Export failed", "The file could not be written:\n" + e.getMessage()
                    + "\n\nYour loan records have not been changed.");
        } catch (DataAccessException e) {
            Alerts.showError("Export failed", "Could not read your loans: " + e.getMessage());
        }
    }

    // =====================================================================================
    // Account actions
    // =====================================================================================

    private void handleEditProfile() {
        Optional<User> updated = new ProfileController(authService)
                .showEditProfileDialog(navigator.getStage(), session.getCurrentUser());
        updated.ifPresent(user -> {
            session.updateCurrentUser(user);
            updateWelcomeMessage();
            view.getFeedbackLabel().showSuccess("Profile saved.");
        });
    }

    private void handleChangePassword() {
        boolean changed = new ProfileController(authService)
                .showChangePasswordDialog(navigator.getStage(), currentUserId());
        if (changed) {
            view.getFeedbackLabel().showSuccess("Password changed. Use your new password next time you log in.");
        }
    }

    private void handleLogout() {
        // Clear the session; showLogin() replaces this whole screen with a fresh login view,
        // so none of this user's data stays on screen.
        session.logout();
        navigator.showLogin("You have been logged out.");
    }
}
