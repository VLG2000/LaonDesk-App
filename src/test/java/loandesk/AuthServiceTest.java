package loandesk;

import loandesk.dao.Database;
import loandesk.dao.JdbcUserDao;
import loandesk.dao.UserCredentials;
import loandesk.model.User;
import loandesk.security.PasswordHasher;
import loandesk.service.AuthService;
import loandesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for signup, login, profile editing, password change and salted hashing,
 * using a temporary SQLite database.
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    private JdbcUserDao userDao;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("auth-test.db"));
        database.initialise();
        userDao = new JdbcUserDao(database);
        auth = new AuthService(userDao, new PasswordHasher(1_000));
    }

    @Test
    @DisplayName("Login trims the username and ignores its case (Alex_1 == alex_1)")
    void loginIgnoresCaseAndWhitespace() {
        User created = auth.signUp(" Alex_1 ", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        User loggedIn = auth.login("  alex_1 ", "Passw0rd!");
        assertEquals(created.getUserId(), loggedIn.getUserId());
        assertEquals("Alex_1", loggedIn.getUsername());
    }

    @Test
    @DisplayName("Invalid: a username that differs only by case is a duplicate")
    void duplicateUsernameRejected() {
        auth.signUp("Alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        assertThrows(ValidationException.class,
                () -> auth.signUp("ALEX_1", "Other1pass!", "Other1pass!", "Someone", "Else"));
    }

    @Test
    @DisplayName("Invalid: wrong password (including wrong case) is rejected")
    void wrongPasswordRejected() {
        auth.signUp("alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        assertThrows(ValidationException.class, () -> auth.login("alex_1", "passw0rd!"));
        assertThrows(ValidationException.class, () -> auth.login("nobody", "Passw0rd!"));
    }

    @Test
    @DisplayName("Passwords are stored as salted PBKDF2 hashes; equal passwords get different salts and hashes")
    void passwordsAreSaltedAndHashed() {
        auth.signUp("user_a", "Passw0rd!", "Passw0rd!", "User", "A");
        auth.signUp("user_b", "Passw0rd!", "Passw0rd!", "User", "B");
        UserCredentials a = userDao.findCredentialsByUsername("user_a").orElseThrow();
        UserCredentials b = userDao.findCredentialsByUsername("user_b").orElseThrow();

        assertEquals(PasswordHasher.ALGORITHM, a.password().algorithm());
        assertNotEquals("Passw0rd!", a.password().hashBase64());
        assertNotEquals(a.password().saltBase64(), b.password().saltBase64());
        assertNotEquals(a.password().hashBase64(), b.password().hashBase64());
    }

    @Test
    @DisplayName("Password change: new password works, old one is rejected, and a new salt is generated")
    void changePassword() {
        User user = auth.signUp("alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        String oldSalt = userDao.findCredentialsById(user.getUserId()).orElseThrow().password().saltBase64();

        auth.changePassword(user.getUserId(), "Passw0rd!", "N3w-secret", "N3w-secret");

        assertEquals(user.getUserId(), auth.login("alex_1", "N3w-secret").getUserId());
        assertThrows(ValidationException.class, () -> auth.login("alex_1", "Passw0rd!"));
        String newSalt = userDao.findCredentialsById(user.getUserId()).orElseThrow().password().saltBase64();
        assertNotEquals(oldSalt, newSalt);
    }

    @Test
    @DisplayName("Password change is rejected for wrong current password, mismatch or reuse")
    void changePasswordRejections() {
        User user = auth.signUp("alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        int id = user.getUserId();
        assertThrows(ValidationException.class, () -> auth.changePassword(id, "Wrong0ne!", "N3w-secret", "N3w-secret"));
        assertThrows(ValidationException.class, () -> auth.changePassword(id, "Passw0rd!", "N3w-secret", "N3w-secreT"));
        assertThrows(ValidationException.class, () -> auth.changePassword(id, "Passw0rd!", "Passw0rd!", "Passw0rd!"));
        // The original password still works because nothing was saved.
        assertEquals(id, auth.login("alex_1", "Passw0rd!").getUserId());
    }

    @Test
    @DisplayName("Profile edit saves trimmed names; invalid names are rejected")
    void updateProfile() {
        User user = auth.signUp("alex_1", "Passw0rd!", "Passw0rd!", "Alex", "Smith");
        User updated = auth.updateProfile(user.getUserId(), "  Alexandra ", " O'Brien-Smith ");
        assertEquals("Alexandra O'Brien-Smith", updated.getFullName());
        assertThrows(ValidationException.class, () -> auth.updateProfile(user.getUserId(), "   ", "Smith"));
    }
}
