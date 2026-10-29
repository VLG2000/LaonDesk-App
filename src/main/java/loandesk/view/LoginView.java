package loandesk.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * VIEW for the login screen.
 *
 */
public class LoginView {

    private final VBox root = new VBox();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField(); // shows dots, not text
    private final Button loginButton = new Button("Log in");
    private final Hyperlink signupLink = new Hyperlink("Create an account");
    private final MessageLabel messageLabel = new MessageLabel();

    public LoginView() {
        buildLayout();
    }

    /** Arranges the controls in a centred "card". */
    private void buildLayout() {
        Label title = new Label("LoanDesk");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Your personal equipment-lending register");
        subtitle.getStyleClass().add("subtitle");

        usernameField.setPromptText("e.g. alex_1");
        passwordField.setPromptText("Password");

        // Two-column form: labels on the left, inputs on the right.
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Username:"), usernameField);
        form.addRow(1, new Label("Password:"), passwordField);


        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        HBox signupRow = new HBox(4, new Label("New to LoanDesk?"), signupLink);
        signupRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, title, subtitle, form, loginButton, messageLabel, signupRow);
        card.getStyleClass().add("card");
        card.setMaxWidth(400);

        root.getChildren().add(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
    }

    /** @return the top-level node to place in the Scene */
    public Parent getRoot() {
        return root;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Hyperlink getSignupLink() {
        return signupLink;
    }

    public MessageLabel getMessageLabel() {
        return messageLabel;
    }
}
