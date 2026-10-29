package loandesk.service;

import loandesk.model.User;

/**
 * Remembers who is currently logged in. There is only ever one logged-in user at a time.
 */
public class Session {

    /** The logged-in user, or null when nobody is logged in. */
    private User currentUser;

    /**
     * Starts a session after a successful login.
     *
     * @param user the authenticated user
     */
    public void login(User user) {
        this.currentUser = user;
    }

    /** Ends the session: forgets the user completely. */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * @return true if someone is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * @return the logged-in user
     * @throws IllegalStateException if nobody is logged in
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in.");
        }
        return currentUser;
    }

    /**
     * Replaces the stored user object, e.g. after the profile name changed.
     *
     * @param updatedUser the user with new details (must be the same account)
     */
    public void updateCurrentUser(User updatedUser) {
        if (currentUser == null || currentUser.getUserId() != updatedUser.getUserId()) {
            throw new IllegalStateException("Cannot update a different user's session.");
        }
        this.currentUser = updatedUser;
    }
}
