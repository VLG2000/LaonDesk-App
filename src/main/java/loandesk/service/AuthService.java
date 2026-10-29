package loandesk.service;

import loandesk.dao.DataAccessException;
import loandesk.dao.UserCredentials;
import loandesk.dao.UserDao;
import loandesk.model.User;
import loandesk.security.PasswordHasher;
import loandesk.validation.ValidationException;
import loandesk.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for accounts: signup, login, profile editing and password change.
 *
 */
public class AuthService {

    /** Same message for unknown username and wrong password, so attackers cannot tell which. */
    public static final String INVALID_LOGIN_MESSAGE = "Incorrect username or password.";

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;

    /**
     * @param userDao        where accounts are stored
     * @param passwordHasher used to hash and verify passwords
     */
    public AuthService(UserDao userDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Creates a new account.
     *
     * @param username        username as typed (will be trimmed)
     * @param password        password as typed (NOT trimmed)
     * @param confirmPassword the password typed a second time
     * @param firstName       first name as typed (will be trimmed)
     * @param lastName        last name as typed (will be trimmed)
     * @return the newly created user
     * @throws ValidationException if any rule fails or the username is taken
     * @throws DataAccessException if the database cannot be updated
     */
    public User signUp(String username, String password, String confirmPassword,
                       String firstName, String lastName) {
        // 1. Check every field and collect all the problems.
        List<String> errors = new ArrayList<>();
        Validator.addIfError(errors, Validator.checkUsername(username));
        Validator.addIfError(errors, Validator.checkPassword(password));
        if (password != null && !password.equals(confirmPassword)) {
            errors.add("Password and confirmation do not match.");
        }
        Validator.addIfError(errors, Validator.checkPersonName(firstName, "First name"));
        Validator.addIfError(errors, Validator.checkPersonName(lastName, "Last name"));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 2. Check the username is not taken (the DAO comparison ignores case).
        String cleanUsername = Validator.clean(username);
        if (userDao.usernameExists(cleanUsername)) {
            throw new ValidationException("The username \"" + cleanUsername
                    + "\" is already taken (usernames are not case-sensitive).");
        }

        // 3. Hash the password with a new random salt and save. The plaintext is never stored.
        return userDao.insert(cleanUsername, Validator.clean(firstName), Validator.clean(lastName),
                passwordHasher.hash(password));
    }

    /**
     * Checks a username/password pair.
     *
     * @param username username as typed (trimmed, case ignored)
     * @param password password as typed (case-sensitive)
     * @return the logged-in user
     * @throws ValidationException if the credentials are wrong or missing
     */
    public User login(String username, String password) {
        String cleanUsername = Validator.clean(username);
        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter both your username and password.");
        }
        Optional<UserCredentials> credentials = userDao.findCredentialsByUsername(cleanUsername);
        if (credentials.isEmpty() || !passwordHasher.verify(password, credentials.get().password())) {
            throw new ValidationException(INVALID_LOGIN_MESSAGE);
        }
        return credentials.get().user();
    }

    /**
     * Changes the first and last name of a user, using the same rules as signup.
     *
     * @param userId    the logged-in user's ID
     * @param firstName new first name as typed
     * @param lastName  new last name as typed
     * @return the updated user
     * @throws ValidationException if a name is invalid
     * @throws DataAccessException if saving fails
     */
    public User updateProfile(int userId, String firstName, String lastName) {
        List<String> errors = new ArrayList<>();
        Validator.addIfError(errors, Validator.checkPersonName(firstName, "First name"));
        Validator.addIfError(errors, Validator.checkPersonName(lastName, "Last name"));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String cleanFirst = Validator.clean(firstName);
        String cleanLast = Validator.clean(lastName);
        if (!userDao.updateName(userId, cleanFirst, cleanLast)) {
            throw new DataAccessException("Your account could not be found, so the profile was not saved.");
        }
        // Read the saved row back so the caller gets exactly what is in the database.
        return userDao.findCredentialsById(userId)
                .map(UserCredentials::user)
                .orElseThrow(() -> new DataAccessException("Your account could not be reloaded."));
    }

    /**
     * Changes a user's password (advanced task 4.1).
     *
     * @param userId          the logged-in user's ID
     * @param currentPassword the existing password (must be correct)
     * @param newPassword     the new password (must follow signup rules and differ from current)
     * @param confirmPassword the new password typed again
     * @throws ValidationException if any check fails (nothing is saved)
     * @throws DataAccessException if saving fails
     */
    public void changePassword(int userId, String currentPassword, String newPassword,
                               String confirmPassword) {
        UserCredentials credentials = userDao.findCredentialsById(userId)
                .orElseThrow(() -> new DataAccessException("Your account could not be found."));

        List<String> errors = new ArrayList<>();
        boolean currentIsCorrect = currentPassword != null && !currentPassword.isEmpty()
                && passwordHasher.verify(currentPassword, credentials.password());
        if (currentPassword == null || currentPassword.isEmpty()) {
            errors.add("Please enter your current password.");
        } else if (!currentIsCorrect) {
            errors.add("Current password is incorrect.");
        }

        String ruleError = Validator.checkPassword(newPassword);
        if (ruleError != null) {
            errors.add("New p" + ruleError.substring(1)); // "Password ..." → "New password ..."
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            errors.add("New password and confirmation do not match.");
        }
        if (currentIsCorrect && currentPassword.equals(newPassword)) {
            errors.add("New password must be different from your current password.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // hash() always creates a NEW random salt, as the spec requires.
        if (!userDao.updatePassword(userId, passwordHasher.hash(newPassword))) {
            throw new DataAccessException("Your account could not be found, so the password was not changed.");
        }
    }
}
