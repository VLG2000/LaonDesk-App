package loandesk.controller;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import loandesk.dao.DataAccessException;
import loandesk.validation.ValidationException;
import loandesk.view.MessageLabel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared helper for the "form in a dialog" pattern used by New/Edit loan, Edit profile
 * and Change password.
 */
final class FormDialog {

    private FormDialog() {
    }

    /**
     * @param owner      parent window (the dialog is centred on it and blocks it)
     * @param title      window title
     * @param header     header text at the top of the dialog
     * @param content    the form view
     * @param errorLabel where to show validation/database errors
     * @param saveAction validates and saves; returns the saved result
     * @param <T>        type of the saved result
     * @return the saved result, or empty if the user cancelled
     */
    static <T> Optional<T> show(Window owner, String title, String header, Parent content,
                                MessageLabel errorLabel, Supplier<T> saveAction) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setResizable(true);
        dialog.getDialogPane().setContent(content);
        // Reuse the main window's CSS so the dialog looks the same.
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // A one-element array lets the lambda below store the result.
        Object[] result = new Object[1];

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                result[0] = saveAction.get();
            } catch (ValidationException e) {
                errorLabel.showError(e.getMessage());
                event.consume(); // keep the dialog open
            } catch (DataAccessException e) {
                errorLabel.showError("Saving failed: " + e.getMessage()
                        + "\nPreviously saved data has not been changed.");
                event.consume();
            }
        });

        dialog.showAndWait(); // blocks until Save succeeded or Cancel was clicked

        @SuppressWarnings("unchecked") // we only ever store a T in result[0]
        T saved = (T) result[0];
        return Optional.ofNullable(saved);
    }
}
