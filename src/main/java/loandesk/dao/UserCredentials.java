package loandesk.dao;

import loandesk.model.User;
import loandesk.security.HashedPassword;

/**
 * A user together with their stored password hash. Only the DAO and {@code AuthService}
 * use this; the rest of the application only ever sees the plain {@link User}.
 *
 * @param user     the account details
 * @param password the stored hash, salt and parameters
 */
public record UserCredentials(User user, HashedPassword password) {
}
