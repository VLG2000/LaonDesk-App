package loandesk.controller;

import loandesk.dao.DataAccessException;
import loandesk.model.User;
import loandesk.service.AuthService;
import loandesk.service.Session;
import loandesk.validation.ValidationException;
import loandesk.view.LoginView;

/**
 * CONTROLLER for the login screen: connects the view's buttons to {@link AuthService}.
 */
public class LoginController {

    private final LoginView view;
    private final SceneNavigator navigator;
    private final AuthService authService;
    private final Session session;

    public LoginController(LoginView view, SceneNavigator navigator, AuthService authService, Session session) {
        this.view = view;
        this.navigator = navigator;
        this.authService = authService;
        this.session = session;

        // Event-driven programming: register what happens when each control is used.
        view.getLoginButton().setOnAction(event -> handleLogin());
        view.getSignupLink().setOnAction(event -> navigator.showSignup());
    }

    /** Tries to log in; on success opens the dashboard, otherwise stays here with a message. */
    private void handleLogin() {
        try {
            User user = authService.login(view.getUsernameField().getText(), view.getPasswordField().getText());
            session.login(user);
            navigator.showDashboard();
        } catch (ValidationException e) {
            // Wrong/missing credentials: show why, clear the password, stay on this screen.
            view.getMessageLabel().showError(e.getMessage());
            view.getPasswordField().clear();
            view.getPasswordField().requestFocus();
        } catch (DataAccessException e) {
            view.getMessageLabel().showError("Could not log in because of a database problem: " + e.getMessage());
        }
    }
}
