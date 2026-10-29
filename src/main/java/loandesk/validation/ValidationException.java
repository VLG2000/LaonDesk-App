package loandesk.validation;

import java.util.List;

/**
 * Thrown when user input breaks a rule (e.g. username too short, duplicate item code,
 * wrong password). It can carry several messages so the user sees every problem at once
 * instead of fixing them one by one.
 */
public class ValidationException extends RuntimeException {

    /** All problems found, one message per problem. */
    private final List<String> errors;

    /**
     * @param error a single error message
     */
    public ValidationException(String error) {
        this(List.of(error));
    }

    /**
     * @param errors one or more error messages
     */
    public ValidationException(List<String> errors) {
        // getMessage() returns all messages, one per line.
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors); // defensive, unmodifiable copy
    }

    /**
     * @return an unmodifiable list of the individual error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}
