package loandesk.service;

import loandesk.model.Loan;
import loandesk.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the plain-text (.txt) export of a user's loans.
 */
public class ExportService {

    /** Placeholder for an empty return date or note (same as in the GUI). */
    public static final String EMPTY_PLACEHOLDER = "-";

    private static final String SEPARATOR = "----------------------------------------";

    /**
     * Builds the export text.
     *
     * @param user       the logged-in user (username and current full name are printed)
     * @param loans      ALL of the user's loans
     * @param exportDate today's date, used both for the header and for status calculation
     * @return the full report text
     */
    public String buildReport(User user, List<Loan> loans, LocalDate exportDate) {
        // Sort again here so the export order is guaranteed, whatever list is passed in.
        List<Loan> ordered = new ArrayList<>(loans);
        ordered.sort(Loan.NEWEST_FIRST);

        // System.lineSeparator() gives the normal line ending for the user's OS.
        String nl = System.lineSeparator();
        StringBuilder text = new StringBuilder();
        text.append("LoanDesk - Loan Export").append(nl);
        text.append("======================").append(nl);
        text.append("Username:    ").append(user.getUsername()).append(nl);
        text.append("Full name:   ").append(user.getFullName()).append(nl);
        text.append("Export date: ").append(exportDate).append(nl);
        text.append("Total loans: ").append(ordered.size()).append(nl);
        text.append(nl);

        if (ordered.isEmpty()) {
            text.append("There are no loan records.").append(nl);
            return text.toString();
        }

        for (Loan loan : ordered) {
            text.append(SEPARATOR).append(nl);
            text.append("Loan ID:       ").append(loan.getLoanId()).append(nl);
            text.append("Item code:     ").append(loan.getItemCode()).append(nl);
            text.append("Item name:     ").append(loan.getItemName()).append(nl);
            text.append("Borrower:      ").append(loan.getBorrowerName()).append(nl);
            text.append("Category:      ").append(loan.getCategory().getLabel()).append(nl);
            text.append("Loan date:     ").append(loan.getLoanDate()).append(nl);
            text.append("Due date:      ").append(loan.getDueDate()).append(nl);
            text.append("Return date:   ").append(orPlaceholder(loan.getReturnDate())).append(nl);
            text.append("Status:        ").append(loan.getStatus(exportDate).getLabel()).append(nl);
            text.append("Note:          ").append(formatNote(loan.getNote(), nl)).append(nl);
        }
        text.append(SEPARATOR).append(nl);
        return text.toString();
    }

    /**
     * Writes the report to a file using UTF-8. If the file exists it is replaced (the GUI's
     * file chooser has already asked the user to confirm overwriting).
     *
     * @param file    destination file
     * @param content report text
     * @throws IOException if the file cannot be written (the caller shows an error)
     */
    public void writeReport(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /** Returns "-" for a missing value, otherwise the value as text. */
    private static String orPlaceholder(Object value) {
        return value == null ? EMPTY_PLACEHOLDER : value.toString();
    }

    /** Notes can contain line breaks; indent following lines so the layout stays readable. */
    private static String formatNote(String note, String nl) {
        if (note == null || note.isBlank()) {
            return EMPTY_PLACEHOLDER;
        }
        return note.replace("\r\n", "\n").replace("\n", nl + "               ");
    }
}
