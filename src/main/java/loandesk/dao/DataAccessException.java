package loandesk.dao;

/**
 * Thrown when a database operation fails (file locked, disk full, corrupt file, ...).
 */
public class DataAccessException extends RuntimeException {

    /**
     * @param message description of what failed
     * @param cause   the original exception (may be null)
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message description of what failed
     */
    public DataAccessException(String message) {
        super(message);
    }
}
