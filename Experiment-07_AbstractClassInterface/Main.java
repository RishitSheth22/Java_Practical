abstract class Shape {
    abstract double area();

    void describe() {
        System.out.println("This is a shape.");
    }
}

interface Printable {
    void print();

    default void showType() {
        System.out.println("Printable object");
    }
}

class Circle extends Shape implements Printable {
    private final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    @Override
    double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public void print() {
        System.out.println("Circle area: " + area());
    }
}

public class Main {
    public static void main(String[] args) {
        Circle circle = new Circle(3);
        circle.describe();
        circle.print();
        circle.showType();
    }
}
