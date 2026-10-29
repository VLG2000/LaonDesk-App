package loandesk.dao;

import loandesk.model.Loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for loans (see {@link UserDao} for an explanation of the DAO pattern).
 *
 */
public interface LoanDao {

    /**
     * @param ownerId the logged-in user's ID
     * @return all of this user's loans, newest loan date first, then highest ID first
     */
    List<Loan> findAllByOwner(int ownerId);

    /**
     * @return the loan only if it exists AND belongs to {@code ownerId}
     */
    Optional<Loan> findByIdAndOwner(int loanId, int ownerId);

    /**
     * Checks the "one unreturned loan per item code" rule.
     *
     * @param ownerId        user whose register is checked
     * @param itemCode       normalised (uppercase) item code
     * @param excludeLoanId  a loan to ignore (the one being edited), or 0 to ignore none
     * @return true if another unreturned loan with this code exists
     */
    boolean hasOutstandingLoanWithCode(int ownerId, String itemCode, int excludeLoanId);

    /**
     * Inserts a new loan (its loanId field is ignored; the database generates one).
     *
     * @return the saved loan with its new ID
     */
    Loan insert(Loan loan);

    /**
     * Updates the editable fields of an UNRETURNED loan owned by {@code loan.getOwnerId()}.
     * Loan ID, owner and loan date are never changed.
     *
     * @return true if exactly one row was updated
     */
    boolean update(Loan loan);

    /**
     * Records the return date for an unreturned loan owned by {@code ownerId}.
     *
     * @return true if exactly one row was updated (false if not found, not owned or already returned)
     */
    boolean markReturned(int loanId, int ownerId, LocalDate returnDate);

    /**
     * Deletes one loan owned by {@code ownerId}.
     *
     * @return true if exactly one row was deleted
     */
    boolean delete(int loanId, int ownerId);
}
