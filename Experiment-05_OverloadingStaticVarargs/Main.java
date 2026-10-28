class Calculator {
    static int objectCount = 0;

    Calculator() {
        objectCount++;
    }

    int add(int a, int b) {
        return a + b;
    }

    double add(double a, double b) {
        return a + b;
    }

    int sum(int... values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }
}

public class Main {
    public static void main(String[] args) {
        Calculator c = new Calculator();
        System.out.println("Integer add: " + c.add(2, 3));
        System.out.println("Double add: " + c.add(2.5, 3.2));
        System.out.println("Varargs sum: " + c.sum(1, 2, 3, 4));
        System.out.println("Objects created: " + Calculator.objectCount);
    }
}
