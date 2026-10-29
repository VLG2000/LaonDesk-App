package loandesk.dao;

import loandesk.model.User;
import loandesk.security.HashedPassword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * JDBC/SQLite implementation of {@link UserDao}.
 */
public class JdbcUserDao implements UserDao {

    /** Column list shared by the SELECT queries. */
    private static final String SELECT_USER = """
            SELECT user_id, username, first_name, last_name,
                   password_hash, password_salt, hash_algorithm, hash_iterations
            FROM users""";

    private final Database database;

    /**
     * @param database provides JDBC connections
     */
    public JdbcUserDao(Database database) {
        this.database = database;
    }

    @Override
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet results = statement.executeQuery()) {
                return results.next(); // if row exists username is stored
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the username: " + e.getMessage(), e);
        }
    }

    @Override
    public User insert(String username, String firstName, String lastName, HashedPassword password) {
        String sql = """
                INSERT INTO users (username, first_name, last_name,
                                   password_hash, password_salt, hash_algorithm, hash_iterations)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, password.hashBase64());
            statement.setString(5, password.saltBase64());
            statement.setString(6, password.algorithm());
            statement.setInt(7, password.iterations());
            statement.executeUpdate();

            // Ask SQLite for the ID it just generated (must use the same connection).
            int newId = lastInsertedId(connection);
            return new User(newId, username, firstName, lastName);
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the account: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UserCredentials> findCredentialsByUsername(String username) {
        return findOne(SELECT_USER + " WHERE username = ?", username);
    }

    @Override
    public Optional<UserCredentials> findCredentialsById(int userId) {
        return findOne(SELECT_USER + " WHERE user_id = ?", userId);
    }

    @Override
    public boolean updateName(int userId, String firstName, String lastName) {

        String sql = "UPDATE users SET first_name = ?, last_name = ? WHERE user_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setInt(3, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the profile: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updatePassword(int userId, HashedPassword password) {
        String sql = """
                UPDATE users
                SET password_hash = ?, password_salt = ?, hash_algorithm = ?, hash_iterations = ?
                WHERE user_id = ?""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, password.hashBase64());
            statement.setString(2, password.saltBase64());
            statement.setString(3, password.algorithm());
            statement.setInt(4, password.iterations());
            statement.setInt(5, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the new password: " + e.getMessage(), e);
        }
    }


    /**
     * Runs a query that returns at most one user, binding a single parameter.
     */
    private Optional<UserCredentials> findOne(String sql, Object parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(results));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the account: " + e.getMessage(), e);
        }
    }

    /** Converts the current row of a ResultSet into a {@link UserCredentials} object. */
    private static UserCredentials mapRow(ResultSet row) throws SQLException {
        User user = new User(
                row.getInt("user_id"),
                row.getString("username"),
                row.getString("first_name"),
                row.getString("last_name"));
        HashedPassword password = new HashedPassword(
                row.getString("hash_algorithm"),
                row.getInt("hash_iterations"),
                row.getString("password_salt"),
                row.getString("password_hash"));
        return new UserCredentials(user, password);
    }

    /** Returns the auto-generated ID of the last INSERT on this connection. */
    static int lastInsertedId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT last_insert_rowid()")) {
            results.next();
            return results.getInt(1);
        }
    }
}
