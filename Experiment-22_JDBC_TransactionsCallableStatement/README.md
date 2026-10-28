# Experiment 22 — JDBC Transactions and CallableStatement

Prerequisites: MySQL, database `java_practicals`, and MySQL Connector/J on the classpath. Update the password in `Main.java`.

Create the stored procedure before running the CallableStatement section:

```sql
USE java_practicals;
DELIMITER //
CREATE PROCEDURE GetAccount(IN accountId INT)
BEGIN
  SELECT id, balance FROM accounts WHERE id = accountId;
END //
DELIMITER ;
```

The program demonstrates disabling auto-commit, committing a successful transfer, rolling back on error, and calling a stored procedure. Run Experiment 21's setup first if needed.

## Aim
To demonstrate jdbc transactions and stored procedures using callablestatement.
