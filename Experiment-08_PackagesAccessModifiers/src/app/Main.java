package app;

import college.Student;

public class Main {
    public static void main(String[] args) {
        Student student = new Student();
        student.display();
        System.out.println("Public field from another package: " + student.name);
        // Protected, package-private and private fields are not directly accessible here.
    }
}
