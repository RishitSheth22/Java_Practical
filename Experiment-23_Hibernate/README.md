# Experiment 23 — Hibernate database write

## Requirements
- JDK 17+
- Maven
- MySQL database named `java_practicals`

Update `YOUR_MYSQL_PASSWORD` in `src/main/java/Main.java`. The program uses Hibernate annotations and creates/updates the table automatically.

Run from this folder:

```bash
mvn compile
mvn exec:java -Dexec.mainClass=Main
```

If the `exec:java` goal is unavailable in your Maven setup, add the Maven Exec Plugin or run `Main` from IntelliJ/VS Code after importing this folder as a Maven project. Do not commit real credentials.

## Aim
To demonstrate hibernate program to write to a database.
