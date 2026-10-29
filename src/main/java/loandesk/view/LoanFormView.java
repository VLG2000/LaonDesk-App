package loandesk.view;

import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import loandesk.model.Category;
import loandesk.model.Loan;
import loandesk.model.LoanInput;
import loandesk.validation.Validator;

import java.time.LocalDate;
import java.util.Locale;

/**
 * VIEW for the New loan / Edit loan form (shown inside a dialog).
 */
public class LoanFormView {

    private final GridPane root = new GridPane();
    private final Label loanIdValue = new Label();
    private final Label loanDateValue = new Label();
    private final TextField itemCodeField = new TextField();
    private final TextField itemNameField = new TextField();
    private final TextField borrowerField = new TextField();
    private final ComboBox<Category> categoryBox = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
    private final DatePicker dueDatePicker = new DatePicker();
    private final TextArea noteArea = new TextArea();
    private final Label noteCounter = new Label();
    private final MessageLabel errorLabel = new MessageLabel();

    public LoanFormView() {
        buildLayout();
    }

    private void buildLayout() {
        root.setHgap(10);
        root.setVgap(8);

        itemCodeField.setPromptText("e.g. DRILL-01");
        // Show the item code in UPPERCASE while typing (it is also uppercased when saved).
        itemCodeField.setTextFormatter(new TextFormatter<String>(change -> {
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));
        itemNameField.setPromptText("e.g. Cordless drill");
        borrowerField.setPromptText("Who is borrowing it?");
        categoryBox.setPromptText("Select a category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        // Picking from the calendar only (no typing) avoids invalid typed dates.
        dueDatePicker.setEditable(false);
        dueDatePicker.setPromptText("Choose the due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);

        noteArea.setPromptText("Optional (up to " + Validator.NOTE_MAX + " characters)");
        noteArea.setWrapText(true);
        noteArea.setPrefRowCount(4);
        // Live character counter, turning red when over the limit.
        noteArea.textProperty().addListener((obs, oldText, newText) -> updateNoteCounter());
        updateNoteCounter();

        int row = 0;
        root.addRow(row++, new Label("Loan ID:"), loanIdValue);
        root.addRow(row++, new Label("Loan date:"), loanDateValue);
        root.addRow(row++, new Label("Item code:"), itemCodeField);
        root.add(hint("1-" + Validator.ITEM_CODE_MAX + " letters, digits or hyphens"), 1, row++);
        root.addRow(row++, new Label("Item name:"), itemNameField);
        root.addRow(row++, new Label("Borrower name:"), borrowerField);
        root.addRow(row++, new Label("Category:"), categoryBox);
        root.addRow(row++, new Label("Due date:"), dueDatePicker);
        root.addRow(row++, new Label("Note:"), noteArea);
        root.add(noteCounter, 1, row++);
        root.add(errorLabel, 0, row, 2, 1); // spans both columns
        root.setPrefWidth(460);
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        return label;
    }

    /** Updates the "x / 250" counter (counts the trimmed text, like the validation does). */
    private void updateNoteCounter() {
        int length = Validator.clean(noteArea.getText()).length();
        noteCounter.setText(length + " / " + Validator.NOTE_MAX + " characters");
        noteCounter.getStyleClass().removeAll("hint", "error-text");
        noteCounter.getStyleClass().add(length > Validator.NOTE_MAX ? "error-text" : "hint");
    }

    /**
     * Greys out calendar days before {@code earliest} so the user cannot pick a due date
     * before the loan date (the service still validates this too).
     *
     * @param earliest first selectable date
     */
    public void restrictDueDateFrom(LocalDate earliest) {
        dueDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(earliest));
            }
        });
    }

    /**
     * Fills the form with an existing loan's values (for editing).
     *
     * @param loan the loan being edited
     */
    public void fill(Loan loan) {
        loanIdValue.setText(String.valueOf(loan.getLoanId()));
        loanDateValue.setText(loan.getLoanDate().toString());
        itemCodeField.setText(loan.getItemCode());
        itemNameField.setText(loan.getItemName());
        borrowerField.setText(loan.getBorrowerName());
        categoryBox.setValue(loan.getCategory());
        dueDatePicker.setValue(loan.getDueDate());
        noteArea.setText(loan.getNote());
    }

    /**
     * Collects the current form values. No validation happens here; the service does it.
     *
     * @return the raw values typed/selected by the user
     */
    public LoanInput readInput() {
        return new LoanInput(itemCodeField.getText(), itemNameField.getText(), borrowerField.getText(),
                categoryBox.getValue(), dueDatePicker.getValue(), noteArea.getText());
    }

    public void setLoanIdText(String text) {
        loanIdValue.setText(text);
    }

    public void setLoanDateText(String text) {
        loanDateValue.setText(text);
    }

    public TextField getItemCodeField() {
        return itemCodeField;
    }

    public MessageLabel getErrorLabel() {
        return errorLabel;
    }

    public Parent getRoot() {
        return root;
    }
}
