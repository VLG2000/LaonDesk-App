package loandesk.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database file: creates the tables on first run and hands out
 * JDBC connections.
 *
 * <p>Each DAO method asks for a new connection and closes it with try-with-resources
 * as soon as it is finished. Opening a local SQLite connection is very cheap, and this
 * guarantees no connection is ever left open (so logout/exit is always clean).</p>
 */
public class Database {

    // "IF NOT EXISTS" means running these on an existing database changes nothing, so
    // saved accounts and loans are never reset or overwritten.

    /**
     * Users table. "COLLATE NOCASE" makes both the UNIQUE check and "WHERE username = ?"
     */
    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                user_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                username        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                first_name      TEXT    NOT NULL,
                last_name       TEXT    NOT NULL,
                password_hash   TEXT    NOT NULL,
                password_salt   TEXT    NOT NULL,
                hash_algorithm  TEXT    NOT NULL,
                hash_iterations INTEGER NOT NULL
            )""";

    /**
     * Loans table. Each loan references its owner by the stable user_id (foreign key).
     * AUTOINCREMENT guarantees a loan ID is never reused, even after a loan is deleted.
     * Dates are stored as ISO text (yyyy-mm-dd), which also sorts correctly as text.
     */
    private static final String CREATE_LOANS_TABLE = """
            CREATE TABLE IF NOT EXISTS loans (
                loan_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER NOT NULL REFERENCES users(user_id),
                item_code     TEXT    NOT NULL,
                item_name     TEXT    NOT NULL,
                borrower_name TEXT    NOT NULL,
                category      TEXT    NOT NULL
                              CHECK (category IN ('TOOLS', 'ELECTRONICS', 'OUTDOOR', 'OTHER')),
                loan_date     TEXT    NOT NULL,
                due_date      TEXT    NOT NULL,
                return_date   TEXT,
                note          TEXT    NOT NULL DEFAULT ''
            )""";

    /**
     * Extra safety net for the "same item cannot be lent twice at once" rule: a partial
     * unique index allows only ONE unreturned loan per (user, item code). The service
     * checks this first and shows a friendly message; the index guarantees it even if
     * the check were somehow bypassed.
     */
    private static final String CREATE_OUTSTANDING_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_outstanding_loan_per_code
                ON loans(user_id, item_code) WHERE return_date IS NULL""";

    /** Absolute location of the database file. */
    private final Path databaseFile;

    /**
     * @param databaseFile path of the SQLite file (relative paths are resolved against
     *                     the current working directory)
     */
    public Database(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
    }

    /**
     * @return the absolute path of the database file (shown in error messages)
     */
    public Path getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Opens the database (SQLite creates the file if it is missing) and creates any missing
     * tables. No users or loans are inserted. If the file exists but is not a valid database,
     * SQLite reports an error and this method throws; the file is never deleted or replaced.
     *
     * @throws DataAccessException if the folder cannot be created or the database cannot be opened
     */
    public void initialise() {
        // folder check
        Path folder = databaseFile.getParent();
        if (folder != null) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                throw new DataAccessException("Cannot create the folder " + folder + ": " + e.getMessage(), e);
            }
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // Run all three statements as one transaction: either all succeed or none do.
            connection.setAutoCommit(false);
            try {
                statement.executeUpdate(CREATE_USERS_TABLE);
                statement.executeUpdate(CREATE_LOANS_TABLE);
                statement.executeUpdate(CREATE_OUTSTANDING_INDEX);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    /**
     * Opens a new JDBC connection to the database.
     *
     * @return an open connection with foreign-key checking switched on
     * @throws SQLException if the connection cannot be opened
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            // SQLite ignores REFERENCES constraints unless this is enabled per connection.
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            connection.close(); // do not leak the connection if the pragma fails
            throw e;
        }
        return connection;
    }
}
