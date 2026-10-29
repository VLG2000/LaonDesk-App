package loandesk;

import loandesk.dao.Database;
import loandesk.dao.JdbcLoanDao;
import loandesk.dao.JdbcUserDao;
import loandesk.dao.LoanDao;
import loandesk.model.Category;
import loandesk.model.Loan;
import loandesk.model.LoanInput;
import loandesk.model.LoanStatus;
import loandesk.security.PasswordHasher;
import loandesk.service.AuthService;
import loandesk.service.LoanService;
import loandesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for loan business rules against a REAL SQLite database stored in a temporary
 * folder (JUnit's @TempDir). The application's own data/loandesk.db is never touched.
 * Each test gets a fresh, empty database.
 */
class LoanServiceTest {

    /** Fixed reference dates (the example dates from the specification). */
    private static final LocalDate OCT_12 = LocalDate.of(2026, 10, 12);
    private static final LocalDate OCT_13 = LocalDate.of(2026, 10, 13);

    @TempDir
    Path tempDir;

    private LoanDao loanDao;
    private int alexId;
    private int samId;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("test.db"));
        database.initialise();
        loanDao = new JdbcLoanDao(database);
        // Few iterations keep the tests fast; the real app uses PasswordHasher.DEFAULT_ITERATIONS.
        AuthService auth = new AuthService(new JdbcUserDao(database), new PasswordHasher(1_000));
        alexId = auth.signUp("alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith").getUserId();
        samId = auth.signUp("sam", "Passw0rd!", "Passw0rd!", "Sam", "Lee").getUserId();
    }

    /** A LoanService whose "today" is the given date. */
    private LoanService serviceOn(LocalDate today) {
        return new LoanService(loanDao, Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static LoanInput drill(String code, LocalDate due) {
        return new LoanInput(code, "Cordless drill", "Jordan", Category.TOOLS, due, "");
    }

    @Test
    @DisplayName("Valid: a new loan gets a positive ID, uppercase code, today's loan date and no return date")
    void createLoanValid() {
        Loan loan = serviceOn(OCT_12).createLoan(alexId, drill(" drill-01 ", OCT_12));
        assertTrue(loan.getLoanId() > 0);
        assertEquals("DRILL-01", loan.getItemCode());
        assertEquals(OCT_12, loan.getLoanDate());
        assertNull(loan.getReturnDate());
        assertEquals(1, serviceOn(OCT_12).getLoans(alexId).size());
    }

    @Test
    @DisplayName("Invalid: an outstanding DRILL-01 blocks a new loan for drill-01 (case-insensitive)")
    void duplicateOutstandingCodeRejected() {
        LoanService service = serviceOn(OCT_12);
        service.createLoan(alexId, drill("DRILL-01", OCT_13));
        assertThrows(ValidationException.class, () -> service.createLoan(alexId, drill("drill-01", OCT_13)));
        assertEquals(1, service.getLoans(alexId).size(), "the rejected loan must not be saved");
    }

    @Test
    @DisplayName("Different users may use the same item code in their own registers")
    void sameCodeAllowedForDifferentUsers() {
        LoanService service = serviceOn(OCT_12);
        service.createLoan(alexId, drill("DRILL-01", OCT_13));
        Loan samLoan = service.createLoan(samId, drill("DRILL-01", OCT_13));
        assertEquals(samId, samLoan.getOwnerId());
    }

    @Test
    @DisplayName("Status: due today is On loan; after one day it is Overdue; returning sets Returned and today's date")
    void statusAndReturnBehaviour() {
        Loan loan = serviceOn(OCT_12).createLoan(alexId, drill("DRILL-01", OCT_12));
        assertEquals(LoanStatus.ON_LOAN, loan.getStatus(OCT_12));
        assertEquals(LoanStatus.OVERDUE, loan.getStatus(OCT_13));

        serviceOn(OCT_13).markReturned(alexId, loan.getLoanId());
        Loan returned = serviceOn(OCT_13).getLoans(alexId).get(0);
        assertEquals(OCT_13, returned.getReturnDate());
        assertEquals(LoanStatus.RETURNED, returned.getStatus(OCT_13));
    }

    @Test
    @DisplayName("After returning DRILL-01 it can be lent again with a new ID, and the old loan stays in history")
    void relendAfterReturn() {
        Loan first = serviceOn(OCT_12).createLoan(alexId, drill("DRILL-01", OCT_12));
        serviceOn(OCT_13).markReturned(alexId, first.getLoanId());
        Loan second = serviceOn(OCT_13).createLoan(alexId, drill("drill-01", OCT_13));

        assertNotEquals(first.getLoanId(), second.getLoanId());
        List<Loan> loans = serviceOn(OCT_13).getLoans(alexId);
        assertEquals(2, loans.size());
        // Sorted newest loan date first: the new loan comes before the returned one.
        assertEquals(second.getLoanId(), loans.get(0).getLoanId());
        assertEquals(first.getLoanId(), loans.get(1).getLoanId());
    }

    @Test
    @DisplayName("Returned loans cannot be returned again or edited")
    void returnedLoanIsLocked() {
        LoanService service = serviceOn(OCT_12);
        Loan loan = service.createLoan(alexId, drill("DRILL-01", OCT_12));
        service.markReturned(alexId, loan.getLoanId());

        assertThrows(ValidationException.class, () -> service.markReturned(alexId, loan.getLoanId()));
        assertThrows(ValidationException.class,
                () -> service.updateLoan(alexId, loan.getLoanId(), drill("DRILL-09", OCT_13)));
    }

    @Test
    @DisplayName("Editing keeps ID/loan date, is not a duplicate of itself, but cannot take another outstanding code")
    void editLoanRules() {
        LoanService service = serviceOn(OCT_12);
        Loan drillOne = service.createLoan(alexId, drill("DRILL-01", OCT_12));
        service.createLoan(alexId, drill("DRILL-02", OCT_12));

        // Same code as itself: allowed.
        LoanInput edited = new LoanInput("drill-01", "Hammer drill", "Jordan", Category.TOOLS, OCT_13, "new note");
        Loan updated = serviceOn(OCT_13).updateLoan(alexId, drillOne.getLoanId(), edited);
        assertEquals(drillOne.getLoanId(), updated.getLoanId());
        assertEquals(OCT_12, updated.getLoanDate(), "loan date must not change when editing");
        assertEquals("Hammer drill", updated.getItemName());

        // Code of the other unreturned loan: rejected.
        assertThrows(ValidationException.class,
                () -> service.updateLoan(alexId, drillOne.getLoanId(), drill("DRILL-02", OCT_13)));
    }

    @Test
    @DisplayName("Ownership: another user cannot edit, return or delete Alex's loan")
    void ownershipIsEnforced() {
        LoanService service = serviceOn(OCT_12);
        Loan loan = service.createLoan(alexId, drill("DRILL-01", OCT_12));
        int id = loan.getLoanId();

        assertThrows(ValidationException.class, () -> service.updateLoan(samId, id, drill("X-1", OCT_12)));
        assertThrows(ValidationException.class, () -> service.markReturned(samId, id));
        assertThrows(ValidationException.class, () -> service.deleteLoan(samId, id));
        assertTrue(service.getLoans(samId).isEmpty(), "Sam sees none of Alex's loans");

        Loan unchanged = service.getLoans(alexId).get(0);
        assertEquals("DRILL-01", unchanged.getItemCode());
        assertNull(unchanged.getReturnDate());
    }

    @Test
    @DisplayName("Deleting a loan removes it and frees its item code")
    void deleteLoan() {
        LoanService service = serviceOn(OCT_12);
        Loan loan = service.createLoan(alexId, drill("DRILL-01", OCT_12));
        service.deleteLoan(alexId, loan.getLoanId());
        assertTrue(service.getLoans(alexId).isEmpty());
        service.createLoan(alexId, drill("DRILL-01", OCT_12)); // no exception: code is free again
    }

    @Test
    @DisplayName("Loans on the same date are listed with the highest loan ID first")
    void sortOrderSameDate() {
        LoanService service = serviceOn(OCT_12);
        Loan a = service.createLoan(alexId, drill("A-1", OCT_12));
        Loan b = service.createLoan(alexId, drill("B-1", OCT_12));
        List<Loan> loans = service.getLoans(alexId);
        assertEquals(b.getLoanId(), loans.get(0).getLoanId());
        assertEquals(a.getLoanId(), loans.get(1).getLoanId());
    }
}
