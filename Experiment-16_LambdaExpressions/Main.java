interface Operation {
    int calculate(int a, int b);
}

public class Main {
    public static void main(String[] args) {
        Operation add = (a, b) -> a + b;
        Operation multiply = (a, b) -> a * b;

        System.out.println("Addition: " + add.calculate(4, 3));
        System.out.println("Multiplication: " + multiply.calculate(4, 3));

        Runnable task = () -> System.out.println("Lambda Runnable executed");
        task.run();
    }
}
