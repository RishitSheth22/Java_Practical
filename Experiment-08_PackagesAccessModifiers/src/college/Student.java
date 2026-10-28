package college;

public class Student {
    public String name = "Riya";
    protected int rollNo = 12;
    int semester = 3; // package-private
    private double marks = 88.5;

    public void display() {
        System.out.println("Name: " + name);
        System.out.println("Roll: " + rollNo);
        System.out.println("Semester: " + semester);
        System.out.println("Marks: " + marks);
    }
}
