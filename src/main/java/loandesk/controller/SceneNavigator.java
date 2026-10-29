package loandesk.controller;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import loandesk.service.AuthService;
import loandesk.service.ExportService;
import loandesk.service.LoanService;
import loandesk.service.Session;
import loandesk.view.DashboardView;
import loandesk.view.LoginView;
import loandesk.view.SignupView;

import java.net.URL;

/**
 * Switches between the three main screens (login, signup, dashboard) in the one window.
 */
public class SceneNavigator {

    private final Stage stage;
    private final Scene scene;
    private final Session session;
    private final AuthService authService;
    private final LoanService loanService;
    private final ExportService exportService;

    /**
     * @param stage         the main application window
     * @param session       who is logged in
     * @param authService   account logic
     * @param loanService   loan logic
     * @param exportService export logic
     */
    public SceneNavigator(Stage stage, Session session, AuthService authService,
                          LoanService loanService, ExportService exportService) {
        this.stage = stage;
        this.session = session;
        this.authService = authService;
        this.loanService = loanService;
        this.exportService = exportService;

        // One Scene for the whole app; we only swap its root node between screens.
        this.scene = new Scene(new Pane(), 1150, 700);
        URL css = getClass().getResource("/loandesk/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    /**
     * Shows the login screen.
     *
     * @param message optional green message (e.g. "Account created"), or null
     */
    public void showLogin(String message) {
        LoginView view = new LoginView();
        new LoginController(view, this, authService, session);
        if (message != null) {
            view.getMessageLabel().showSuccess(message);
        }
        scene.setRoot(view.getRoot());
        stage.setTitle("LoanDesk - Log in");
        view.getUsernameField().requestFocus();
    }

    /** Shows the signup screen. */
    public void showSignup() {
        SignupView view = new SignupView();
        new SignupController(view, this, authService);
        scene.setRoot(view.getRoot());
        stage.setTitle("LoanDesk - Create account");
        view.getUsernameField().requestFocus();
    }

    /** Shows the dashboard for the user currently stored in the session. */
    public void showDashboard() {
        DashboardView view = new DashboardView();
        new DashboardController(view, this, session, authService, loanService, exportService);
        scene.setRoot(view.getRoot());
        stage.setTitle("LoanDesk - Dashboard (" + session.getCurrentUser().getUsername() + ")");
    }

    /** @return the main window (used as the owner of dialogs and file choosers) */
    public Stage getStage() {
        return stage;
    }
}
