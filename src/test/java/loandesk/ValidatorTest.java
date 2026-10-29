package loandesk;

import loandesk.model.Category;
import loandesk.model.LoanInput;
import loandesk.validation.ValidationException;
import loandesk.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure validation rules (no database or GUI needed).
 */
class ValidatorTest {

    /** Fixed reference date so results never depend on the day the tests run. */
    private static final LocalDate LOAN_DATE = LocalDate.of(2026, 10, 12);

    @Test
    @DisplayName("Boundary: usernames of 3 and 20 characters are valid; 2 and 21 are not")
    void usernameLengthBoundaries() {
        assertNull(Validator.checkUsername("abc"));
        assertNull(Validator.checkUsername("a".repeat(20)));
        assertNotNull(Validator.checkUsername("ab"));
        assertNotNull(Validator.checkUsername("a".repeat(21)));
    }

    @Test
    @DisplayName("Valid: surrounding whitespace is trimmed from usernames before checking")
    void usernameIsTrimmed() {
        assertNull(Validator.checkUsername("  Alex_1  "));
    }

    @Test
    @DisplayName("Invalid: usernames with spaces or symbols are rejected")
    void usernameInvalidCharacters() {
        assertNotNull(Validator.checkUsername("alex smith"));
        assertNotNull(Validator.checkUsername("alex-1"));
    }

    @Test
    @DisplayName("Password rules: missing symbol, whitespace and too short are rejected; a strong password is accepted")
    void passwordRules() {
        assertNull(Validator.checkPassword("Passw0rd!"));
        assertNotNull(Validator.checkPassword("Passw0rd"), "missing symbol");
        assertNotNull(Validator.checkPassword("Pass w0rd!"), "contains a space");
        assertNotNull(Validator.checkPassword(" Passw0rd!"), "leading space is rejected, not trimmed");
        assertNotNull(Validator.checkPassword("Pa0!abc"), "only 7 characters");
        assertNull(Validator.checkPassword("Pa0!abcd"), "exactly 8 characters is enough");
    }

    @Test
    @DisplayName("Names allow spaces, hyphens and apostrophes; whitespace-only and 51 characters are rejected")
    void personNames() {
        assertNull(Validator.checkPersonName(" Mary-Jane O'Neil ", "First name"));
        assertNull(Validator.checkPersonName("a".repeat(50), "First name"));
        assertNotNull(Validator.checkPersonName("a".repeat(51), "First name"));
        assertNotNull(Validator.checkPersonName(" \t\n ", "First name"));
    }

    @Test
    @DisplayName("Boundary: a note of 250 characters (after trimming) is accepted, 251 is rejected")
    void noteLengthBoundary() {
        assertNull(Validator.checkNote("   " + "x".repeat(250) + "\n  "));
        assertNotNull(Validator.checkNote("x".repeat(251)));
        // Inner line breaks count towards the limit.
        assertNotNull(Validator.checkNote("x".repeat(125) + "\n" + "x".repeat(125)));
    }

    @Test
    @DisplayName("Boundary: due date equal to the loan date is valid, one day earlier is invalid")
    void dueDateBoundary() {
        assertNull(Validator.checkDueDate(LOAN_DATE, LOAN_DATE));
        assertNotNull(Validator.checkDueDate(LOAN_DATE.minusDays(1), LOAN_DATE));
        assertNotNull(Validator.checkDueDate(null, LOAN_DATE));
    }

    @Test
    @DisplayName("validateLoan trims text and uppercases the item code")
    void validateLoanNormalisesInput() {
        LoanInput raw = new LoanInput("  drill-01 ", " Cordless drill ", " Sam ", Category.TOOLS,
                LOAN_DATE, "  ");
        LoanInput clean = Validator.validateLoan(raw, LOAN_DATE);
        assertEquals("DRILL-01", clean.itemCode());
        assertEquals("Cordless drill", clean.itemName());
        assertEquals("Sam", clean.borrowerName());
        assertEquals("", clean.note());
    }

    @Test
    @DisplayName("Invalid: whitespace-only required fields and missing category are all reported together")
    void validateLoanReportsAllErrors() {
        LoanInput raw = new LoanInput(" ", "\t", "\n", null, null, null);
        ValidationException e = assertThrows(ValidationException.class,
                () -> Validator.validateLoan(raw, LOAN_DATE));
        assertEquals(5, e.getErrors().size()); // code, name, borrower, category, due date
        assertTrue(e.getMessage().contains("Item code is required."));
    }
}
