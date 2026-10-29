package loandesk.validation;

import loandesk.model.LoanInput;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * All input validation rules from the specification, in one place.
 */
public final class Validator {

    // Limits from the specification
    public static final int NAME_MAX = 50;
    public static final int PASSWORD_MIN = 8;
    public static final int ITEM_CODE_MAX = 20;
    public static final int ITEM_NAME_MAX = 80;
    public static final int BORROWER_MAX = 60;
    public static final int NOTE_MAX = 250;

    /** The only symbols allowed in a password (at least one is required). */
    public static final String PASSWORD_SYMBOLS = "!@#$%^&*?._-";

    // Regular expressions
    // Username: 3-20 chars of A-Z, a-z, 0-9 or underscore.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,20}");

    // Password may only contain letters, digits and the listed symbols.
    // Inside [...] the '-' is placed last so it means a literal hyphen, not a range.
    private static final Pattern PASSWORD_ALLOWED = Pattern.compile("[A-Za-z0-9!@#$%^&*?._-]+");

    // Names: letters (any language, \p{L}, plus accent marks \p{M}), spaces, hyphens and
    // apostrophes (both the straight ' and the curly ’).
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{M} '\\u2019-]+");

    // Item code: 1-20 English letters, digits or hyphens.
    private static final Pattern ITEM_CODE_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,20}");

    /** Utility class: no instances. */
    private Validator() {
    }

    /**
     * Removes surrounding whitespace (spaces, tabs, line breaks). A null value becomes "",
     * so a field that was never filled in is treated the same as an empty one.
     *
     * @param value raw text (may be null)
     * @return the trimmed text, never null
     */
    public static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * Helper used by the services: adds {@code error} to the list only if it is not null.
     *
     * @param errors list collecting problems
     * @param error  result of a {@code checkXxx} call
     */
    public static void addIfError(List<String> errors, String error) {
        if (error != null) {
            errors.add(error);
        }
    }


    // Account rules


    /**
     * Username rule: 3-20 characters, letters/digits/underscore only, after trimming.
     *
     * @param rawUsername the username as typed
     * @return null if valid, otherwise an error message
     */
    public static String checkUsername(String rawUsername) {
        String username = clean(rawUsername);
        if (username.isEmpty()) {
            return "Username is required.";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username must be 3-20 characters using only letters, digits and underscores.";
        }
        return null;
    }

    /**
     * Password rule: at least 8 characters, with an uppercase letter, a lowercase letter,
     * a digit and one of the allowed symbols. No other characters (including spaces) are
     * allowed. The password is NOT trimmed: a space anywhere makes it invalid.
     *
     * @param password the password exactly as typed
     * @return null if valid, otherwise an error message describing what is missing
     */
    public static String checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (!PASSWORD_ALLOWED.matcher(password).matches()) {
            return "Password may only contain letters, digits and these symbols: "
                    + PASSWORD_SYMBOLS + " (no spaces).";
        }

        // Collect every missing requirement so the user can fix them all in one go.
        List<String> missing = new ArrayList<>();
        if (password.length() < PASSWORD_MIN) {
            missing.add("at least " + PASSWORD_MIN + " characters");
        }
        if (password.chars().noneMatch(c -> c >= 'A' && c <= 'Z')) {
            missing.add("an uppercase letter");
        }
        if (password.chars().noneMatch(c -> c >= 'a' && c <= 'z')) {
            missing.add("a lowercase letter");
        }
        if (password.chars().noneMatch(c -> c >= '0' && c <= '9')) {
            missing.add("a digit");
        }
        if (password.chars().noneMatch(c -> PASSWORD_SYMBOLS.indexOf(c) >= 0)) {
            missing.add("a symbol (" + PASSWORD_SYMBOLS + ")");
        }
        if (!missing.isEmpty()) {
            return "Password needs " + String.join(", ", missing) + ".";
        }
        return null;
    }

    /**
     * First/last name rule: 1-50 characters after trimming; letters, spaces, hyphens and
     * apostrophes only, and at least one letter (so "-" alone is not a name).
     *
     * @param rawName    the name as typed
     * @param fieldLabel "First name" or "Last name", used in the error message
     * @return null if valid, otherwise an error message
     */
    public static String checkPersonName(String rawName, String fieldLabel) {
        String name = clean(rawName);
        if (name.isEmpty()) {
            return fieldLabel + " is required.";
        }
        if (name.length() > NAME_MAX) {
            return fieldLabel + " must be at most " + NAME_MAX + " characters.";
        }
        if (!NAME_PATTERN.matcher(name).matches() || name.chars().noneMatch(Character::isLetter)) {
            return fieldLabel + " may only contain letters, spaces, hyphens and apostrophes.";
        }
        return null;
    }


    // Loan rules


    /**
     * Normalises an item code for saving and comparing: trim + uppercase.
     * So " drill-01 " and "DRILL-01" become the same code.
     *
     * @param rawCode code as typed
     * @return normalised code
     */
    public static String normaliseItemCode(String rawCode) {
        // Locale.ROOT avoids surprises in languages with unusual case rules (e.g. Turkish "i").
        return clean(rawCode).toUpperCase(Locale.ROOT);
    }

    /**
     * Item code rule: 1-20 characters of English letters, digits and hyphens.
     *
     * @param rawCode code as typed
     * @return null if valid, otherwise an error message
     */
    public static String checkItemCode(String rawCode) {
        String code = clean(rawCode);
        if (code.isEmpty()) {
            return "Item code is required.";
        }
        if (!ITEM_CODE_PATTERN.matcher(code).matches()) {
            return "Item code must be 1-" + ITEM_CODE_MAX
                    + " characters using only English letters, digits and hyphens.";
        }
        return null;
    }

    /**
     * Generic rule for a required text field with a maximum length (item name, borrower).
     * Text that is only spaces/tabs/line breaks counts as empty.
     *
     * @param rawText    text as typed
     * @param fieldLabel name of the field for the message
     * @param maxLength  maximum allowed length after trimming
     * @return null if valid, otherwise an error message
     */
    public static String checkRequiredText(String rawText, String fieldLabel, int maxLength) {
        String text = clean(rawText);
        if (text.isEmpty()) {
            return fieldLabel + " is required.";
        }
        if (text.length() > maxLength) {
            return fieldLabel + " must be at most " + maxLength + " characters (currently "
                    + text.length() + ").";
        }
        return null;
    }

    /**
     * Note rule: optional, at most 250 characters after trimming. Inner spaces and line
     * breaks count towards the limit (Java's length() counts each one as a character).
     *
     * @param rawNote note as typed (may be null/empty)
     * @return null if valid, otherwise an error message
     */
    public static String checkNote(String rawNote) {
        int length = clean(rawNote).length();
        if (length > NOTE_MAX) {
            return "Note must be at most " + NOTE_MAX + " characters (currently " + length + ").";
        }
        return null;
    }

    /**
     * Due date rule: required, and must be on or after the loan date
     * (due on the same day as the loan is allowed).
     *
     * @param dueDate  chosen due date (may be null)
     * @param loanDate the loan's loan date
     * @return null if valid, otherwise an error message
     */
    public static String checkDueDate(LocalDate dueDate, LocalDate loanDate) {
        if (dueDate == null) {
            return "Due date is required.";
        }
        if (dueDate.isBefore(loanDate)) {
            return "Due date cannot be before the loan date (" + loanDate + ").";
        }
        return null;
    }

    /**
     * Checks every field of a loan form and, if all are valid, returns a cleaned-up copy
     * (trimmed text and uppercase item code) ready to be saved.
     *
     * @param input    the raw form values
     * @param loanDate the loan date the due date is compared with
     * @return the normalised input
     * @throws ValidationException listing every problem if any field is invalid
     */
    public static LoanInput validateLoan(LoanInput input, LocalDate loanDate) {
        List<String> errors = new ArrayList<>();
        addIfError(errors, checkItemCode(input.itemCode()));
        addIfError(errors, checkRequiredText(input.itemName(), "Item name", ITEM_NAME_MAX));
        addIfError(errors, checkRequiredText(input.borrowerName(), "Borrower name", BORROWER_MAX));
        if (input.category() == null) {
            errors.add("Please select a category.");
        }
        addIfError(errors, checkDueDate(input.dueDate(), loanDate));
        addIfError(errors, checkNote(input.note()));

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new LoanInput(
                normaliseItemCode(input.itemCode()),
                clean(input.itemName()),
                clean(input.borrowerName()),
                input.category(),
                input.dueDate(),
                clean(input.note()));
    }
}
