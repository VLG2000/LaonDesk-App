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
import loandesk.validation.Validator;

/**
 * VIEW for the signup (create account) screen. Shows the rules next to the fields so the
 * user knows what is expected before they press the button.
 */
public class SignupView {

    private final VBox root = new VBox();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final Button createButton = new Button("Create account");
    private final Hyperlink backToLoginLink = new Hyperlink("Back to log in");
    private final MessageLabel messageLabel = new MessageLabel();

    public SignupView() {
        buildLayout();
    }

    private void buildLayout() {
        Label title = new Label("Create a LoanDesk account");
        title.getStyleClass().add("title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(6);
        int row = 0;
        form.addRow(row++, new Label("Username:"), usernameField);
        form.add(hint("3-20 characters: letters, digits and underscores. Not case-sensitive."), 1, row++);
        form.addRow(row++, new Label("Password:"), passwordField);
        form.add(hint("At least " + Validator.PASSWORD_MIN + " characters with an uppercase letter, "
                + "a lowercase letter, a digit and a symbol (" + Validator.PASSWORD_SYMBOLS + ")."), 1, row++);
        form.addRow(row++, new Label("Confirm password:"), confirmPasswordField);
        form.addRow(row++, new Label("First name:"), firstNameField);
        form.addRow(row++, new Label("Last name:"), lastNameField);
        form.add(hint("1-50 characters; letters, spaces, hyphens and apostrophes."), 1, row);

        createButton.setDefaultButton(true);
        createButton.setMaxWidth(Double.MAX_VALUE);

        HBox loginRow = new HBox(4, new Label("Already have an account?"), backToLoginLink);
        loginRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, title, form, createButton, messageLabel, loginRow);
        card.getStyleClass().add("card");
        card.setMaxWidth(560);

        root.getChildren().add(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
    }

    /** Creates a small grey helper text under a field. */
    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setWrapText(true);
        label.setMaxWidth(340);
        return label;
    }

    public Parent getRoot() {
        return root;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public PasswordField getConfirmPasswordField() {
        return confirmPasswordField;
    }

    public TextField getFirstNameField() {
        return firstNameField;
    }

    public TextField getLastNameField() {
        return lastNameField;
    }

    public Button getCreateButton() {
        return createButton;
    }

    public Hyperlink getBackToLoginLink() {
        return backToLoginLink;
    }

    public MessageLabel getMessageLabel() {
        return messageLabel;
    }
}
