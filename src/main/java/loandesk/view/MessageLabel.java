package loandesk.view;

import javafx.scene.control.Label;

/**
 * A Label used for inline feedback under a form: red for errors, green for success.
 * The colours come from the CSS classes "error-text" and "success-text" in styles.css.
 */
public class MessageLabel extends Label {

    public MessageLabel() {
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Shows an error message in red.
     *
     * @param message the text to show
     */
    public void showError(String message) {
        apply(message, "error-text");
    }

    /**
     * Shows a success/info message in green.
     *
     * @param message the text to show
     */
    public void showSuccess(String message) {
        apply(message, "success-text");
    }

    /** Removes any message. */
    public void clear() {
        apply("", null);
    }

    private void apply(String message, String styleClass) {
        getStyleClass().removeAll("error-text", "success-text");
        if (styleClass != null) {
            getStyleClass().add(styleClass);
        }
        setText(message);
    }
}
