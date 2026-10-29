package loandesk.view;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import loandesk.model.User;

/**
 * VIEW for the Edit profile dialog. The username is shown as a plain label because it
 * cannot be changed; only first and last name are editable.
 */
public class ProfileView {

    private final GridPane root = new GridPane();
    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final MessageLabel errorLabel = new MessageLabel();

    /**
     * @param user the logged-in user whose details pre-fill the form
     */
    public ProfileView(User user) {
        root.setHgap(10);
        root.setVgap(8);

        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());

        Label hint = new Label("1-50 characters; letters, spaces, hyphens and apostrophes.");
        hint.getStyleClass().add("hint");

        root.addRow(0, new Label("Username:"), new Label(user.getUsername() + "  (cannot be changed)"));
        root.addRow(1, new Label("First name:"), firstNameField);
        root.addRow(2, new Label("Last name:"), lastNameField);
        root.add(hint, 1, 3);
        root.add(errorLabel, 0, 4, 2, 1);
        root.setPrefWidth(420);
    }

    public Parent getRoot() {
        return root;
    }

    public TextField getFirstNameField() {
        return firstNameField;
    }

    public TextField getLastNameField() {
        return lastNameField;
    }

    public MessageLabel getErrorLabel() {
        return errorLabel;
    }
}
