package loandesk.model;

import java.time.LocalDate;

/**
 * The values a user types into the New/Edit loan form.
 *
 * @param itemCode     raw item code as typed
 * @param itemName     raw item name as typed
 * @param borrowerName raw borrower name as typed
 * @param category     selected category (null if nothing selected)
 * @param dueDate      selected due date (null if nothing selected)
 * @param note         optional note text
 */
public record LoanInput(String itemCode, String itemName, String borrowerName,
                        Category category, LocalDate dueDate, String note) {
}
