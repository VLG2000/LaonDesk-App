package loandesk.controller;

import javafx.stage.Window;
import loandesk.model.User;
import loandesk.service.AuthService;
import loandesk.view.PasswordView;
import loandesk.view.ProfileView;

import java.util.Optional;

/**
 * CONTROLLER for the Edit profile and Change password dialogs.
 */
public class ProfileController {

    private final AuthService authService;

    /**
     * @param authService account business logic
     */
    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Lets the user edit their first and last name.
     *
     * @param owner parent window
     * @param user  the logged-in user
     * @return the updated user, or empty if cancelled
     */
    public Optional<User> showEditProfileDialog(Window owner, User user) {
        ProfileView view = new ProfileView(user);
        return FormDialog.show(owner, "LoanDesk - Edit profile", "Edit your name",
                view.getRoot(), view.getErrorLabel(),
                () -> authService.updateProfile(user.getUserId(),
                        view.getFirstNameField().getText(), view.getLastNameField().getText()));
    }

    /**
     * Lets the user change their password.
     *
     * @param owner  parent window
     * @param userId the logged-in user's ID
     * @return true if the password was changed, false if cancelled
     */
    public boolean showChangePasswordDialog(Window owner, int userId) {
        PasswordView view = new PasswordView();
        Optional<Boolean> changed = FormDialog.show(owner, "LoanDesk - Change password",
                "Change your password", view.getRoot(), view.getErrorLabel(),
                () -> {
                    authService.changePassword(userId,
                            view.getCurrentPasswordField().getText(),
                            view.getNewPasswordField().getText(),
                            view.getConfirmPasswordField().getText());
                    return Boolean.TRUE;
                });
        return changed.orElse(false);
    }
}
