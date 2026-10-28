import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class Main {
    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Student.class);
        configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/java_practicals");
        configuration.setProperty("hibernate.connection.username", "root");
        configuration.setProperty("hibernate.connection.password", "YOUR_MYSQL_PASSWORD");
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("hibernate.show_sql", "true");

        try (SessionFactory factory = configuration.buildSessionFactory();
             Session session = factory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(new Student(101, "Asha"));
            transaction.commit();
            System.out.println("Student saved using Hibernate.");
        } catch (Exception e) {
            System.out.println("Hibernate error: " + e.getMessage());
        }
    }
}
