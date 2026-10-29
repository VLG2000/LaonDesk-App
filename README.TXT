LoanDesk - COSC1295 Advanced Programming, Assignment 2 (2026 S2)
=================================================================

s4205220, Vishnu Gopakumar

[Y] Signup and login with validation
[Y] Dashboard and profile editing
[Y] Loan creation and validation
[Y] Duplicate outstanding-loan prevention
[Y] Loan editing, return and deletion
[Y] Viewing loans and calculating status
[Y] User isolation and logout
[Y] JDBC database persistence and automatic setup
[Y] Text-file export
[Y] MVC
[Y] Additional pattern: DAO (Data Access Object) - package loandesk.dao
    (UserDao/LoanDao interfaces, JdbcUserDao/JdbcLoanDao implementations)
[Y] Advanced password update and salted hashing
[Y] Advanced JUnit tests


SOFTWARE VERSIONS
-----------------
JDK:     21 or newer (compiled with --release 21; developed and tested on OpenJDK 26.0.2)
JavaFX:  21.0.5 (org.openjfx:javafx-controls)
JDBC:    SQLite JDBC driver 3.46.1.3 (org.xerial:sqlite-jdbc)
JUnit:   JUnit Jupiter 5.10.2
Build:   Maven (pom.xml). IntelliJ IDEA's bundled Maven is enough; nothing to install manually.


DEPENDENCY SETUP
----------------
All libraries are declared in pom.xml and downloaded automatically by Maven from
Maven Central the first time the project is opened/built (internet needed once).
No JAR files need to be added by hand.


MAIN CLASS
----------
loandesk.Launcher   (starts loandesk.LoanDeskApp, the JavaFX Application class)
Launcher exists because JavaFX refuses to start a main class that extends Application
directly when JavaFX is on the classpath instead of the module path.


SETUP / RUN / TEST INSTRUCTIONS
-------------------------------
IntelliJ IDEA:
  1. File > Open... and select the project folder (or its pom.xml) > "Open as Project".
     If IntelliJ asks, choose "Trust project" and "Load Maven project".
  2. Wait for Maven to finish downloading dependencies (progress bar at the bottom).
  3. File > Project Structure > Project: set SDK to a JDK 21 or newer.
  4. Run the app: choose the run configuration "LoanDesk" in the toolbar and press Run,
     or open src/main/java/loandesk/Launcher.java and click the green arrow next to main.
  5. Run the tests: choose the run configuration "All tests" and press Run,
     or right-click src/test/java > "Run 'All Tests'".

Command line (alternative, with Maven installed):
  mvn javafx:run      - starts the application
  mvn test            - runs all JUnit tests

The VM option --enable-native-access=ALL-UNNAMED (already in the run configurations)
only hides a warning newer JDKs print when SQLite loads its native library.


DATABASE LOCATION
-----------------
Default: data/loandesk.db, relative to the working directory. In IntelliJ the working
directory is the project folder, so the file is <project folder>/data/loandesk.db.
No absolute path is hard-coded.

- If the file does not exist, the folder, database and tables are created automatically,
  with no users and no loans.
- An existing database is never reset or overwritten (CREATE TABLE IF NOT EXISTS).
- If the file cannot be opened (e.g. it is not a valid SQLite file), an error is shown
  and the application closes WITHOUT deleting or replacing the file.

Start with an EMPTY test database without touching the normal one:
  - Use the run configuration "LoanDesk (empty test DB)", which adds the VM option
      -Dloandesk.db=data/test-loandesk.db
    (any other file name works too). Delete that file to start empty again.

Schema (SQLite):
  users(user_id PK AUTOINCREMENT, username UNIQUE COLLATE NOCASE, first_name, last_name,
        password_hash, password_salt, hash_algorithm, hash_iterations)
  loans(loan_id PK AUTOINCREMENT, user_id FK -> users, item_code, item_name, borrower_name,
        category, loan_date, due_date, return_date NULL until returned, note)
  Partial unique index on loans(user_id, item_code) WHERE return_date IS NULL
  (database-level guarantee of "one unreturned loan per item code per user").


OPTIONAL DEMO DATABASE (to show "Overdue" without waiting a day)
----------------------------------------------------------------
Normal startup never inserts any data. To see overdue loans:
  1. Run the configuration "Create demo DB" (class loandesk.tools.DemoDataSeeder).
     It creates data/demo-loandesk.db with FICTIONAL data and refuses to run if
     that file already exists, so it never overwrites anything.
  2. Run the configuration "LoanDesk (demo DB)".
  3. Log in with   username: demo_user   password: Demo_pass1
  The demo contains two overdue loans, one due today (On loan) and one returned loan.


MVC
---
Model (no JavaFX code):
  loandesk.model       - User, Loan, LoanInput, Category, LoanStatus (status calculation)
  loandesk.validation  - Validator (all input rules), ValidationException
  loandesk.service     - AuthService, LoanService, ExportService (business rules), Session
  loandesk.dao         - database access (see DAO below)
  loandesk.security    - PasswordHasher (PBKDF2)
View (only builds JavaFX controls, no logic):
  loandesk.view        - LoginView, SignupView, DashboardView, LoanFormView, ProfileView,
                         PasswordView, LoanRow (table row view-model), Alerts, MessageLabel
Controller (handles events, calls the model, updates the view):
  loandesk.controller  - LoginController, SignupController, DashboardController,
                         LoanFormController, ProfileController, FormDialog, SceneNavigator
Flow example: clicking "Mark returned" -> DashboardController.handleMarkReturned()
  -> LoanService.markReturned(userId, loanId) -> LoanDao.markReturned(...) (SQL UPDATE)
  -> controller reloads loans from the database and refreshes the table.


ADDITIONAL DESIGN PATTERN: DAO (Data Access Object)
---------------------------------------------------
Where: loandesk.dao. The interfaces UserDao and LoanDao define the data operations;
JdbcUserDao and JdbcLoanDao implement them with JDBC + SQLite.
Why: the services (AuthService, LoanService) depend only on the interfaces, so business
rules are separated from SQL. The storage technology could be replaced (e.g. MySQL)
by writing new implementations, without changing services, controllers or views.
It also gives one place to enforce security: every JDBC statement uses
PreparedStatement parameters, and every statement that reads/changes a loan includes
"AND user_id = ?", so users can only ever access their own loans, even if the GUI
were bypassed.
(The table selection listener in DashboardController also uses JavaFX's built-in
Observer mechanism to enable/disable buttons, but DAO is the pattern claimed.)


SALTED HASHING (advanced task 4.1)
----------------------------------
Class: loandesk.security.PasswordHasher
Algorithm:      PBKDF2WithHmacSHA256 via javax.crypto.SecretKeyFactory (Java standard library)
Salt:           16 random bytes from java.security.SecureRandom, new for every account
                and generated again on every password change
Iterations:     210,000 (stored per account in hash_iterations)
Output length:  256 bits (32 bytes)
Storage:        hash and salt are Base64 text in users.password_hash / users.password_salt,
                plus hash_algorithm and hash_iterations. No plaintext passwords are stored.
Verification:   the typed password is re-hashed with the stored salt/iterations and compared
                with MessageDigest.isEqual (constant time).
Password update: Dashboard > "Change password" requires the current password and the new
                password twice; checks signup rules and rejects reusing the current password.


JUNIT TESTS (advanced task 4.2)
-------------------------------
Location: src/test/java/loandesk
  ValidatorTest     - username/password/name/note/due-date rules incl. boundary cases
  LoanServiceTest   - duplicate outstanding codes (case-insensitive), same code for different
                      users, due-today vs overdue status, return behaviour, re-lending after
                      return, editing rules, ownership checks, deletion, sort order
  AuthServiceTest   - case/whitespace-insensitive login, duplicate usernames, wrong password,
                      salted hashing, password change (new salt, old password rejected),
                      profile editing
  ExportServiceTest - export header, empty-register message, order, status, UTF-8 file
Tests use fixed reference dates (via java.time.Clock) and a fresh temporary SQLite database
created by JUnit's @TempDir, so they never touch data/loandesk.db and need no GUI.
Run: "All tests" run configuration in IntelliJ, or "mvn test".


KNOWN LIMITATIONS
-----------------
- Table columns are not click-sortable, so the required order (newest loan date first,
  then highest loan ID) is always shown.
- The due date is chosen from the calendar pop-up; typing a date is disabled.
- Statuses are recalculated on login, after changes, on Refresh and before export, but not
  automatically at midnight (as allowed by the specification).
- Deleting a loan is permanent (hard delete) after a confirmation prompt.


REFERENCES AND ACKNOWLEDGEMENTS
-------------------------------
- JavaFX documentation: https://openjfx.io/
- SQLite JDBC driver: https://github.com/xerial/sqlite-jdbc
- Java SecretKeyFactory / PBKDF2: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/SecretKeyFactory.html
- OWASP Password Storage Cheat Sheet (iteration count):
  https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
