package loandesk.service;

import loandesk.dao.DataAccessException;
import loandesk.dao.LoanDao;
import loandesk.model.Loan;
import loandesk.model.LoanInput;
import loandesk.validation.ValidationException;
import loandesk.validation.Validator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for loans: create, edit, return, delete and list.
 */
public class LoanService {

    private final LoanDao loanDao;
    private final Clock clock;

    /**
     * @param loanDao where loans are stored
     * @param clock   source of the current date
     */
    public LoanService(LoanDao loanDao, Clock clock) {
        this.loanDao = loanDao;
        this.clock = clock;
    }

    /**
     * @return today's date according to the service's clock
     */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * @param ownerId the logged-in user's ID
     * @return all the user's loans (including returned ones), newest first
     */
    public List<Loan> getLoans(int ownerId) {
        return loanDao.findAllByOwner(ownerId);
    }

    /**
     * Records a new loan lent today.
     *
     * @param ownerId the logged-in user's ID
     * @param input   values from the form
     * @return the saved loan with its generated ID
     * @throws ValidationException if a field is invalid or the item is already on loan
     * @throws DataAccessException if saving fails
     */
    public Loan createLoan(int ownerId, LoanInput input) {
        LocalDate loanDate = today(); // loan date is always "today" and cannot be chosen
        LoanInput clean = Validator.validateLoan(input, loanDate);

        // An item code may only have ONE unreturned loan per user.
        ensureNotAlreadyOnLoan(ownerId, clean.itemCode(), 0);

        Loan newLoan = new Loan(0, ownerId, clean.itemCode(), clean.itemName(),
                clean.borrowerName(), clean.category(), loanDate, clean.dueDate(),
                null, clean.note());
        return loanDao.insert(newLoan);
    }

    /**
     * Edits an unreturned loan. Loan ID, owner and loan date stay unchanged.
     *
     * @param ownerId the logged-in user's ID
     * @param loanId  the loan to edit
     * @param input   new values from the form
     * @return the loan as saved
     * @throws ValidationException if the loan is not found/not owned, already returned,
     *                             a field is invalid, or the new code is already on loan
     * @throws DataAccessException if saving fails
     */
    public Loan updateLoan(int ownerId, int loanId, LoanInput input) {
        // Load the loan WITH the owner check; another user's loan is simply "not found".
        Loan existing = loanDao.findByIdAndOwner(loanId, ownerId)
                .orElseThrow(() -> new ValidationException("Loan #" + loanId + " was not found."));
        if (existing.isReturned()) {
            throw new ValidationException("Loan #" + loanId + " has been returned and can no longer be edited.");
        }

        // The due date is compared with the ORIGINAL loan date, not today.
        LoanInput clean = Validator.validateLoan(input, existing.getLoanDate());

        // Exclude this loan itself from the duplicate check.
        ensureNotAlreadyOnLoan(ownerId, clean.itemCode(), loanId);

        Loan updated = new Loan(loanId, ownerId, clean.itemCode(), clean.itemName(),
                clean.borrowerName(), clean.category(), existing.getLoanDate(),
                clean.dueDate(), null, clean.note());
        if (!loanDao.update(updated)) {
            throw new ValidationException("Loan #" + loanId + " could not be updated (it may have been returned or deleted).");
        }
        return updated;
    }

    /**
     * Records today as the return date of an unreturned loan.
     *
     * @param ownerId the logged-in user's ID
     * @param loanId  the loan being returned
     * @throws ValidationException if the loan is not found/not owned or already returned
     * @throws DataAccessException if saving fails
     */
    public void markReturned(int ownerId, int loanId) {
        if (!loanDao.markReturned(loanId, ownerId, today())) {
            throw new ValidationException("Loan #" + loanId + " was not found or has already been returned.");
        }
    }

    /**
     * Permanently deletes a loan.
     *
     * @param ownerId the logged-in user's ID
     * @param loanId  the loan to delete
     * @throws ValidationException if the loan is not found or not owned
     * @throws DataAccessException if deleting fails
     */
    public void deleteLoan(int ownerId, int loanId) {
        if (!loanDao.delete(loanId, ownerId)) {
            throw new ValidationException("Loan #" + loanId + " was not found.");
        }
    }

    /** Throws if the user already has an unreturned loan with this (normalised) code. */
    private void ensureNotAlreadyOnLoan(int ownerId, String itemCode, int excludeLoanId) {
        if (loanDao.hasOutstandingLoanWithCode(ownerId, itemCode, excludeLoanId)) {
            throw new ValidationException("Item " + itemCode
                    + " is already on loan. Mark that loan as returned before lending it again.");
        }
    }
}
