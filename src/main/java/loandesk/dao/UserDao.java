package loandesk.dao;

import loandesk.model.User;
import loandesk.security.HashedPassword;

import java.util.Optional;

/**
 * DAO (Data Access Object) interface for user accounts.

 */
public interface UserDao {

    /**
     * @param username trimmed username
     * @return true if an account with this username (ignoring case) already exists
     */
    boolean usernameExists(String username);

    /**
     * Saves a new account.
     *
     * @return the saved user including its generated ID
     */
    User insert(String username, String firstName, String lastName, HashedPassword password);

    /**
     * @param username trimmed username (case is ignored)
     * @return the user and stored hash, or empty if no such user
     */
    Optional<UserCredentials> findCredentialsByUsername(String username);

    /**
     * @param userId the user's ID
     * @return the user and stored hash, or empty if no such user
     */
    Optional<UserCredentials> findCredentialsById(int userId);

    /**
     * Updates first and last name of one user.
     *
     * @return true if a row was updated
     */
    boolean updateName(int userId, String firstName, String lastName);

    /**
     * Replaces a user's password hash and salt.
     *
     * @return true if a row was updated
     */
    boolean updatePassword(int userId, HashedPassword password);
}
