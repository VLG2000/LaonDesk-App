package loandesk.controller;

import loandesk.dao.DataAccessException;
import loandesk.model.User;
import loandesk.service.AuthService;
import loandesk.validation.ValidationException;
import loandesk.view.SignupView;

/**
 * CONTROLLER for the signup screen.
 */
public class SignupController {

    private final SignupView view;
    private final SceneNavigator navigator;
    private final AuthService authService;

    public SignupController(SignupView view, SceneNavigator navigator, AuthService authService) {
        this.view = view;
        this.navigator = navigator;
        this.authService = authService;

        view.getCreateButton().setOnAction(event -> handleSignup());
        view.getBackToLoginLink().setOnAction(event -> navigator.showLogin(null));
    }

    /** Validates and saves the new account, then returns to login with a success message. */
    private void handleSignup() {
        try {
            User user = authService.signUp(
                    view.getUsernameField().getText(),
                    view.getPasswordField().getText(),
                    view.getConfirmPasswordField().getText(),
                    view.getFirstNameField().getText(),
                    view.getLastNameField().getText());
            // Only reached when the account has really been saved in the database.
            navigator.showLogin("Account \"" + user.getUsername() + "\" created. You can now log in.");
        } catch (ValidationException e) {
            view.getMessageLabel().showError(e.getMessage());
        } catch (DataAccessException e) {
            view.getMessageLabel().showError("The account could not be saved: " + e.getMessage());
        }
    }
}
