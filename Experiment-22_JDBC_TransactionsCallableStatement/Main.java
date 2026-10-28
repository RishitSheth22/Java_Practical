import java.sql.*;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/java_practicals";
    private static final String USER = "root";
    private static final String PASSWORD = "YOUR_MYSQL_PASSWORD";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                        "id INT PRIMARY KEY, balance DECIMAL(10,2))");
                st.executeUpdate("INSERT INTO accounts VALUES (1, 1000.00) " +
                        "ON DUPLICATE KEY UPDATE balance=balance");
                st.executeUpdate("INSERT INTO accounts VALUES (2, 500.00) " +
                        "ON DUPLICATE KEY UPDATE balance=balance");
            }

            con.setAutoCommit(false);
            try (PreparedStatement debit = con.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE id = ?");
                 PreparedStatement credit = con.prepareStatement(
                        "UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
                debit.setBigDecimal(1, new java.math.BigDecimal("100.00"));
                debit.setInt(2, 1);
                debit.executeUpdate();

                credit.setBigDecimal(1, new java.math.BigDecimal("100.00"));
                credit.setInt(2, 2);
                credit.executeUpdate();

                con.commit();
                System.out.println("Transaction committed.");
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Transaction rolled back.");
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

            // CallableStatement example: create this procedure once in MySQL:
            // CREATE PROCEDURE GetAccount(IN accountId INT)
            // SELECT id, balance FROM accounts WHERE id = accountId;
            try (CallableStatement call = con.prepareCall("{CALL GetAccount(?)}")) {
                call.setInt(1, 1);
                try (ResultSet rs = call.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("Account " + rs.getInt("id") +
                                " balance: " + rs.getBigDecimal("balance"));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Create the GetAccount stored procedure before this part: "
                        + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
