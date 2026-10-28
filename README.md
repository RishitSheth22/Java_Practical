# Java Practicals — Experiments 1–24

This repository contains Java programs for the 24 experiments in the supplied practical list. The Mini Project (Experiment 25) is intentionally excluded.

## Contents
1. Primitive data types, variables, conversion and casting
2. Decision-making, loops and branching
3. Sorting using 1D and 2D arrays
4. Classes, objects, constructors, constructor overloading and `this`
5. Method overloading, static members and varargs
6. Inheritance, overriding and `super`
7. Abstract classes, interfaces and default methods
8. Packages and access modifiers
9. StringBuilder and String methods
10. Exception handling and custom exceptions
11. Multithreading with Thread and Runnable
12. Regular expressions
13. Generic classes and methods
14. ArrayList, LinkedList, Vector and HashSet
15. Custom annotation
16. Lambda expressions
17. Stream API
18. Date and Time API
19. Byte, character and buffered streams
20. Path, Files and Java NIO
21. JDBC CRUD with Statement and PreparedStatement
22. JDBC transactions and CallableStatement
23. Hibernate database write
24. JUnit testing

## How to run
For the basic experiments, open the `.java` file in VS Code / IntelliJ / Eclipse, compile it, and run its main class. Most programs use fixed demonstration data to keep revision quick.

```bash
javac Main.java
java Main
```

Some experiments use multiple source files, packages, external libraries, or a database. Follow the README inside that experiment folder.

## Requirements
- JDK 17 or newer recommended
- MySQL and a configured database for Experiments 21–22
- Maven for Experiments 23–24 (and optionally 21–22)

**Note:** Database credentials are placeholders. Update them locally before running JDBC/Hibernate examples. Do not commit real passwords to GitHub.
