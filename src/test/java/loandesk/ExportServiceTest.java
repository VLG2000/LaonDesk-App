package loandesk;

import loandesk.model.Category;
import loandesk.model.Loan;
import loandesk.model.User;
import loandesk.service.ExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the text export (no GUI, no database needed).
 */
class ExportServiceTest {

    private static final LocalDate EXPORT_DATE = LocalDate.of(2026, 10, 13);
    private final ExportService exportService = new ExportService();
    private final User alex = new User(1, "alex_1", "Alex", "Smith");

    @Test
    @DisplayName("Empty register: header is exported with a 'no loan records' message")
    void emptyExport() {
        String report = exportService.buildReport(alex, List.of(), EXPORT_DATE);
        assertTrue(report.contains("alex_1"));
        assertTrue(report.contains("Alex Smith"));
        assertTrue(report.contains("2026-10-13"));
        assertTrue(report.contains("There are no loan records."));
    }

    @Test
    @DisplayName("Loans are exported newest first with status, dash placeholders and UTF-8 text")
    void exportContentAndOrder(@TempDir Path tempDir) throws Exception {
        Loan older = new Loan(1, 1, "DRILL-01", "Cordless drill", "Sam", Category.TOOLS,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), null, "");
        Loan newer = new Loan(2, 1, "CAM-01", "Caméra", "Zoë", Category.ELECTRONICS,
                LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20), null, "Lens cap");

        String report = exportService.buildReport(alex, List.of(older, newer), EXPORT_DATE);
        assertTrue(report.indexOf("CAM-01") < report.indexOf("DRILL-01"), "newest loan first");
        assertTrue(report.contains("Overdue"), "DRILL-01 was due on the 12th, export date is the 13th");
        assertTrue(report.contains("Return date:   -"));
        assertFalse(report.toLowerCase().contains("password"));

        Path file = tempDir.resolve("export.txt");
        exportService.writeReport(file, report);
        assertEquals(report, Files.readString(file, StandardCharsets.UTF_8));
    }
}
