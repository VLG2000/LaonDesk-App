package loandesk.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.geometry.Orientation;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import loandesk.model.LoanStatus;

import java.util.function.Function;

/**
 * VIEW for the main screen after login: welcome message, action buttons, the loans
 * table and a details panel for the selected loan.
 */
public class DashboardView {

    private final BorderPane root = new BorderPane();
    private final Label welcomeLabel = new Label();

    // Loan actions
    private final Button newLoanButton = new Button("New loan");
    private final Button editLoanButton = new Button("Edit loan");
    private final Button markReturnedButton = new Button("Mark returned");
    private final Button deleteLoanButton = new Button("Delete loan");
    private final Button refreshButton = new Button("Refresh");
    private final Button exportButton = new Button("Export all loans");

    // Account actions
    private final Button editProfileButton = new Button("Edit profile");
    private final Button changePasswordButton = new Button("Change password");
    private final Button logoutButton = new Button("Log out");

    private final TableView<LoanRow> loanTable = new TableView<>();
    private final TextArea detailArea = new TextArea();
    private final MessageLabel feedbackLabel = new MessageLabel();

    public DashboardView() {
        buildTable();
        buildLayout();
    }

    /** Creates the table columns, empty-state message and row colouring. */
    private void buildTable() {
        loanTable.getColumns().add(column("Loan ID", LoanRow::loanIdText, 65));
        loanTable.getColumns().add(column("Item code", row -> row.loan().getItemCode(), 95));
        loanTable.getColumns().add(column("Item name", row -> row.loan().getItemName(), 140));
        loanTable.getColumns().add(column("Borrower", row -> row.loan().getBorrowerName(), 120));
        loanTable.getColumns().add(column("Category", row -> row.loan().getCategory().getLabel(), 90));
        loanTable.getColumns().add(column("Loan date", row -> row.loan().getLoanDate().toString(), 90));
        loanTable.getColumns().add(column("Due date", row -> row.loan().getDueDate().toString(), 90));
        loanTable.getColumns().add(column("Return date", LoanRow::returnDateText, 90));
        loanTable.getColumns().add(column("Status", row -> row.status().getLabel(), 80));
        loanTable.getColumns().add(column("Note", LoanRow::noteSummary, 160));

        // Message shown when the table has no rows.
        Label emptyMessage = new Label("You have no loans yet.\nClick \"New loan\" to record an item you have lent.");
        emptyMessage.getStyleClass().add("hint");
        loanTable.setPlaceholder(emptyMessage);

        // Columns share the available width; the last column takes any extra space.
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);


        loanTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(LoanRow row, boolean empty) {
                super.updateItem(row, empty);
                // Rows are recycled by JavaFX, so always remove old classes first.
                getStyleClass().removeAll("overdue-row", "returned-row");
                if (!empty && row != null) {
                    if (row.status() == LoanStatus.OVERDUE) {
                        getStyleClass().add("overdue-row");
                    } else if (row.status() == LoanStatus.RETURNED) {
                        getStyleClass().add("returned-row");
                    }
                }
            }
        });
    }

    /**
     * Creates a text column. {@code getter} says which text to show for each row.
     * Columns are not sortable so the required order (newest first) is always kept.
     */
    private static TableColumn<LoanRow, String> column(String title, Function<LoanRow, String> getter,
                                                       double width) {
        TableColumn<LoanRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    private void buildLayout() {

        welcomeLabel.getStyleClass().add("title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // pushes the buttons to the right
        HBox accountBar = new HBox(8, welcomeLabel, spacer, editProfileButton, changePasswordButton, logoutButton);
        accountBar.setAlignment(Pos.CENTER_LEFT);


        newLoanButton.getStyleClass().add("primary");
        editLoanButton.setTooltip(new Tooltip("Select an unreturned loan to edit it"));
        markReturnedButton.setTooltip(new Tooltip("Select an unreturned loan to record today as its return date"));
        deleteLoanButton.setTooltip(new Tooltip("Select a loan to delete it permanently"));
        refreshButton.setTooltip(new Tooltip("Reload loans and recalculate overdue statuses"));
        HBox loanBar = new HBox(8, new Label("Loans:"), newLoanButton, editLoanButton, markReturnedButton,
                deleteLoanButton, new Separator(Orientation.VERTICAL), refreshButton, exportButton);
        loanBar.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(12, accountBar, loanBar);
        top.setPadding(new Insets(16, 16, 8, 16));


        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(9);
        detailArea.getStyleClass().add("details");
        detailArea.setText("Select a loan to see all of its details.");

        Label detailTitle = new Label("Selected loan details");
        detailTitle.getStyleClass().add("section-title");
        VBox centre = new VBox(8, loanTable, detailTitle, detailArea);
        VBox.setVgrow(loanTable, Priority.ALWAYS); // the table takes the spare height
        centre.setPadding(new Insets(0, 16, 8, 16));


        VBox bottom = new VBox(feedbackLabel);
        bottom.setPadding(new Insets(0, 16, 12, 16));

        root.setTop(top);
        root.setCenter(centre);
        root.setBottom(bottom);
    }

    public Parent getRoot() {
        return root;
    }

    public Label getWelcomeLabel() {
        return welcomeLabel;
    }

    public Button getNewLoanButton() {
        return newLoanButton;
    }

    public Button getEditLoanButton() {
        return editLoanButton;
    }

    public Button getMarkReturnedButton() {
        return markReturnedButton;
    }

    public Button getDeleteLoanButton() {
        return deleteLoanButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public Button getExportButton() {
        return exportButton;
    }

    public Button getEditProfileButton() {
        return editProfileButton;
    }

    public Button getChangePasswordButton() {
        return changePasswordButton;
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public TableView<LoanRow> getLoanTable() {
        return loanTable;
    }

    public TextArea getDetailArea() {
        return detailArea;
    }

    public MessageLabel getFeedbackLabel() {
        return feedbackLabel;
    }
}
