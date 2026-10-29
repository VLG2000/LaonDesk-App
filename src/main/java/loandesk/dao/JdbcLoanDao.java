package loandesk.dao;

import loandesk.model.Category;
import loandesk.model.Loan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC/SQLite implementation of {@link LoanDao}. All values are bound with
 * {@link PreparedStatement} parameters, and every statement that targets a specific user
 * -id
 */
public class JdbcLoanDao implements LoanDao {

    /** Columns read by every SELECT, in a fixed order. */
    private static final String SELECT_LOAN = """
            SELECT loan_id, user_id, item_code, item_name, borrower_name, category,
                   loan_date, due_date, return_date, note
            FROM loans""";

    private final Database database;

    /**
     * @param database provides JDBC connections
     */
    public JdbcLoanDao(Database database) {
        this.database = database;
    }

    @Override
    public List<Loan> findAllByOwner(int ownerId) {
        String sql = SELECT_LOAN + " WHERE user_id = ? ORDER BY loan_date DESC, loan_id DESC";
        List<Loan> loans = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    loans.add(mapRow(results));
                }
            }
            return loans;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load loans: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Loan> findByIdAndOwner(int loanId, int ownerId) {
        String sql = SELECT_LOAN + " WHERE loan_id = ? AND user_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, loanId);
            statement.setInt(2, ownerId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapRow(results)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the loan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasOutstandingLoanWithCode(int ownerId, String itemCode, int excludeLoanId) {
        String sql = """
                SELECT 1 FROM loans
                WHERE user_id = ? AND item_code = ? AND return_date IS NULL AND loan_id <> ?""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            statement.setString(2, itemCode);
            statement.setInt(3, excludeLoanId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check for duplicate loans: " + e.getMessage(), e);
        }
    }

    @Override
    public Loan insert(Loan loan) {
        String sql = """
                INSERT INTO loans (user_id, item_code, item_name, borrower_name, category,
                                   loan_date, due_date, return_date, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, loan.getOwnerId());
            statement.setString(2, loan.getItemCode());
            statement.setString(3, loan.getItemName());
            statement.setString(4, loan.getBorrowerName());
            statement.setString(5, loan.getCategory().name());  // stored as e.g. "TOOLS"
            statement.setString(6, loan.getLoanDate().toString()); // ISO yyyy-mm-dd
            statement.setString(7, loan.getDueDate().toString());
            statement.setString(8, toText(loan.getReturnDate()));
            statement.setString(9, loan.getNote());
            statement.executeUpdate();

            int newId = JdbcUserDao.lastInsertedId(connection);
            // Return a new object carrying the generated ID.
            return new Loan(newId, loan.getOwnerId(), loan.getItemCode(), loan.getItemName(),
                    loan.getBorrowerName(), loan.getCategory(), loan.getLoanDate(),
                    loan.getDueDate(), loan.getReturnDate(), loan.getNote());
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the loan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Loan loan) {
        String sql = """
                UPDATE loans
                SET item_code = ?, item_name = ?, borrower_name = ?, category = ?,
                    due_date = ?, note = ?
                WHERE loan_id = ? AND user_id = ? AND return_date IS NULL""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loan.getItemCode());
            statement.setString(2, loan.getItemName());
            statement.setString(3, loan.getBorrowerName());
            statement.setString(4, loan.getCategory().name());
            statement.setString(5, loan.getDueDate().toString());
            statement.setString(6, loan.getNote());
            statement.setInt(7, loan.getLoanId());
            statement.setInt(8, loan.getOwnerId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the loan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markReturned(int loanId, int ownerId, LocalDate returnDate) {
        String sql = """
                UPDATE loans SET return_date = ?
                WHERE loan_id = ? AND user_id = ? AND return_date IS NULL""";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, returnDate.toString());
            statement.setInt(2, loanId);
            statement.setInt(3, ownerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not record the return: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int loanId, int ownerId) {
        String sql = "DELETE FROM loans WHERE loan_id = ? AND user_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, loanId);
            statement.setInt(2, ownerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the loan: " + e.getMessage(), e);
        }
    }


    /** Converts the current ResultSet row into a {@link Loan}. */
    private static Loan mapRow(ResultSet row) throws SQLException {
        return new Loan(
                row.getInt("loan_id"),
                row.getInt("user_id"),
                row.getString("item_code"),
                row.getString("item_name"),
                row.getString("borrower_name"),
                Category.valueOf(row.getString("category")),
                LocalDate.parse(row.getString("loan_date")),
                LocalDate.parse(row.getString("due_date")),
                parseDate(row.getString("return_date")),
                row.getString("note"));
    }

    /** null-safe LocalDate → text. */
    private static String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** null-safe text → LocalDate (return_date is NULL until the item is returned). */
    private static LocalDate parseDate(String text) {
        return text == null ? null : LocalDate.parse(text);
    }
}
