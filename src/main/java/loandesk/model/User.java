package loandesk.model;

/**
 * A LoanDesk account holder (a "lender").
 */
public final class User {

    /** Stable database ID. Loans are linked to the owner using this number, not the name. */
    private final int userId;

    /** Username exactly as typed at signup (trimmed). Comparisons ignore case. */
    private final String username;

    private final String firstName;
    private final String lastName;

    /**
     * Creates a user object.
     *
     * @param userId    database ID of the user
     * @param username  the username
     * @param firstName first name
     * @param lastName  last name
     */
    public User(int userId, String username, String firstName, String lastName) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    /**
     * @return first and last name joined with a space, e.g. "Alex Smith"
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns a copy of this user with a different first and last name
     * (used after the profile has been edited successfully).
     *
     * @param newFirstName the new first name
     * @param newLastName  the new last name
     * @return a new {@code User} with the same ID and username
     */
    public User withName(String newFirstName, String newLastName) {
        return new User(userId, username, newFirstName, newLastName);
    }

    @Override
    public String toString() {
        return username + " (" + getFullName() + ")";
    }
}
