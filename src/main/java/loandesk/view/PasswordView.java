package loandesk.view;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import loandesk.validation.Validator;

/**
 * VIEW for the Change password dialog (advanced task 4.1): current password and the new
 * password typed twice, all in masked PasswordFields.
 */
public class PasswordView {

    private final GridPane root = new GridPane();
    private final PasswordField currentPasswordField = new PasswordField();
    private final PasswordField newPasswordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final MessageLabel errorLabel = new MessageLabel();

    public PasswordView() {
        root.setHgap(10);
        root.setVgap(8);

        Label hint = new Label("At least " + Validator.PASSWORD_MIN + " characters with an uppercase letter, "
                + "a lowercase letter, a digit and a symbol (" + Validator.PASSWORD_SYMBOLS + ").");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setMaxWidth(280);

        root.addRow(0, new Label("Current password:"), currentPasswordField);
        root.addRow(1, new Label("New password:"), newPasswordField);
        root.addRow(2, new Label("Confirm new password:"), confirmPasswordField);
        root.add(hint, 1, 3);
        root.add(errorLabel, 0, 4, 2, 1);
        root.setPrefWidth(440);
    }

    public Parent getRoot() {
        return root;
    }

    public PasswordField getCurrentPasswordField() {
        return currentPasswordField;
    }

    public PasswordField getNewPasswordField() {
        return newPasswordField;
    }

    public PasswordField getConfirmPasswordField() {
        return confirmPasswordField;
    }

    public MessageLabel getErrorLabel() {
        return errorLabel;
    }
}
