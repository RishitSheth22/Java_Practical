import java.sql.*;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/java_practicals";
    private static final String USER = "root";
    private static final String PASSWORD = "YOUR_MYSQL_PASSWORD";

    public static void main(String[] args) {
        String createTable = "CREATE TABLE IF NOT EXISTS students (" +
                "id INT PRIMARY KEY, name VARCHAR(50), marks INT)";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTable);

            // INSERT using PreparedStatement
            String insert = "INSERT INTO students(id, name, marks) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=VALUES(name), marks=VALUES(marks)";
            try (PreparedStatement ps = connection.prepareStatement(insert)) {
                ps.setInt(1, 1);
                ps.setString(2, "Asha");
                ps.setInt(3, 90);
                ps.executeUpdate();
            }

            // READ using Statement
            try (ResultSet rs = statement.executeQuery("SELECT id, name, marks FROM students")) {
                while (rs.next()) {
                    System.out.println(rs.getInt("id") + " " +
                            rs.getString("name") + " " + rs.getInt("marks"));
                }
            }

            // UPDATE using PreparedStatement
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE students SET marks=? WHERE id=?")) {
                ps.setInt(1, 95);
                ps.setInt(2, 1);
                ps.executeUpdate();
            }

            // DELETE example (commented to preserve the sample row)
            // try (PreparedStatement ps = connection.prepareStatement("DELETE FROM students WHERE id=?")) {
            //     ps.setInt(1, 1);
            //     ps.executeUpdate();
            // }
            System.out.println("CRUD demonstration completed.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
