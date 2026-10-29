package loandesk;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import loandesk.controller.SceneNavigator;
import loandesk.dao.DataAccessException;
import loandesk.dao.Database;
import loandesk.dao.JdbcLoanDao;
import loandesk.dao.JdbcUserDao;
import loandesk.dao.LoanDao;
import loandesk.dao.UserDao;
import loandesk.security.PasswordHasher;
import loandesk.service.AuthService;
import loandesk.service.ExportService;
import loandesk.service.LoanService;
import loandesk.service.Session;
import loandesk.view.Alerts;

import java.nio.file.Path;
import java.time.Clock;


public class LoanDeskApp extends Application {


    public static final String DATABASE_PROPERTY = "loandesk.db";


    public static final String DEFAULT_DATABASE_PATH = "data/loandesk.db";

    /**
     * Standard Java main method; {@code launch} starts the JavaFX runtime which then calls {@link #start}.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Called by JavaFX on the JavaFX Application Thread once the toolkit is ready.
     *
     * @param stage the main window supplied by JavaFX
     */
    @Override
    public void start(Stage stage) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) ->
                Alerts.showError("Unexpected error",
                        "Something went wrong: " + error.getMessage()
                                + "\nPreviously saved data has not been changed."));

        // Locate and open the database
        Path databasePath = resolveDatabasePath();
        Database database = new Database(databasePath);
        try {
            database.initialise();
        } catch (DataAccessException e) {
            Alerts.showError("Database could not be opened",
                    "LoanDesk could not open the database file:\n" + database.getDatabaseFile()
                            + "\n\nThe file has NOT been changed or deleted."
                            + "\n\nDetails: " + e.getMessage());
            Platform.exit();
            return;
        }

        //2. Build the model layer (DAOs + services)
        UserDao userDao = new JdbcUserDao(database);
        LoanDao loanDao = new JdbcLoanDao(database);
        AuthService authService = new AuthService(userDao, new PasswordHasher());
        LoanService loanService = new LoanService(loanDao, Clock.systemDefaultZone());
        ExportService exportService = new ExportService();

        //Showing the first screen
        SceneNavigator navigator = new SceneNavigator(stage, new Session(),
                authService, loanService, exportService);
        navigator.showLogin(null);
        stage.show();
    }

    /**
     * Works out the database location. Uses the {@value #DATABASE_PROPERTY} system property
     * if it was given (handy for starting with an empty test database), otherwise the default
     * relative path. A relative path keeps the project portable between computers.
     *
     * @return the path of the SQLite database file
     */
    static Path resolveDatabasePath() {
        String configured = System.getProperty(DATABASE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_DATABASE_PATH;
        }
        return Path.of(configured.strip());
    }
}
