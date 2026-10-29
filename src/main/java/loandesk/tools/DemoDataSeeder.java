package loandesk.tools;

import loandesk.dao.Database;
import loandesk.dao.JdbcLoanDao;
import loandesk.dao.JdbcUserDao;
import loandesk.dao.LoanDao;
import loandesk.model.Category;
import loandesk.model.Loan;
import loandesk.model.User;
import loandesk.security.PasswordHasher;
import loandesk.service.AuthService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * OPTIONAL helper (not used by normal startup) that creates a SEPARATE demo database
 * containing one fictional user and some historical loans, so the "Overdue" status can be
 * demonstrated without waiting a day.
 */
public final class DemoDataSeeder {

    public static final String DEMO_DATABASE = "data/demo-loandesk.db";

    private DemoDataSeeder() {
    }

    public static void main(String[] args) {
        Path path = Path.of(System.getProperty("loandesk.db", DEMO_DATABASE));
        if (Files.exists(path)) {
            System.out.println("Demo database already exists, nothing changed: " + path.toAbsolutePath());
            return;
        }

        Database database = new Database(path);
        database.initialise();
        AuthService authService = new AuthService(new JdbcUserDao(database), new PasswordHasher());
        User demo = authService.signUp("demo_user", "Demo_pass1", "Demo_pass1", "Demo", "User");

        // Insert loans directly through the DAO so we can use dates in the past
        // (the GUI deliberately only allows loans dated today).
        LoanDao loanDao = new JdbcLoanDao(database);
        LocalDate today = LocalDate.now();
        int id = demo.getUserId();
        loanDao.insert(new Loan(0, id, "DRILL-01", "Cordless drill", "Sam Lee", Category.TOOLS,
                today.minusDays(20), today.minusDays(5), null, "Comes with two batteries."));
        loanDao.insert(new Loan(0, id, "TENT-02", "4-person tent", "Priya Nair", Category.OUTDOOR,
                today.minusDays(10), today.minusDays(1), null, ""));
        loanDao.insert(new Loan(0, id, "CAM-01", "Mirrorless camera", "Jordan Kim", Category.ELECTRONICS,
                today.minusDays(3), today, null, "Due back today, so still On loan."));
        loanDao.insert(new Loan(0, id, "LADDER-01", "Step ladder", "Chris Wong", Category.TOOLS,
                today.minusDays(30), today.minusDays(15), today.minusDays(16), "Returned early."));

        System.out.println("Demo database created at " + database.getDatabaseFile());
        System.out.println("Log in with username demo_user and password Demo_pass1");
    }
}
