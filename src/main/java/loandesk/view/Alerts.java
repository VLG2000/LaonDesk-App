package loandesk.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * Small helper for showing pop-up messages and confirmations, so every screen shows
 * errors in the same consistent way.
 */
public final class Alerts {

    /** Utility class: no instances. */
    private Alerts() {
    }

    /**
     * Shows an error pop-up and waits until the user closes it.
     *
     * @param header  short summary, e.g. "Export failed"
     * @param content details
     */
    public static void showError(String header, String content) {
        show(Alert.AlertType.ERROR, "LoanDesk - Error", header, content);
    }

    /**
     * Shows an information pop-up and waits until the user closes it.
     *
     * @param header  short summary
     * @param content details
     */
    public static void showInfo(String header, String content) {
        show(Alert.AlertType.INFORMATION, "LoanDesk", header, content);
    }

    /**
     * Asks the user a yes/no question.
     *
     * @param title   window title
     * @param header  the question
     * @param content extra details
     * @return true only if the user clicked OK
     */
    public static boolean confirm(String title, String header, String content) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, title, header, content);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String header, String content) {
        create(type, title, header, content).showAndWait();
    }

    private static Alert create(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        // Use a wrapping Label so long messages are never cut off with "...".
        Label body = new Label(content);
        body.setWrapText(true);
        body.setMaxWidth(460);
        alert.getDialogPane().setContent(body);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }
}
