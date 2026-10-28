public class Main {
    public static void main(String[] args) {
        int number = 7;
        if (number % 2 == 0) {
            System.out.println(number + " is even");
        } else {
            System.out.println(number + " is odd");
        }

        System.out.print("Multiplication table: ");
        for (int i = 1; i <= 5; i++) {
            System.out.print(number * i + " ");
        }
        System.out.println();

        int sum = 0;
        int i = 1;
        while (i <= 5) {
            sum += i;
            i++;
        }
        System.out.println("Sum from 1 to 5: " + sum);

        for (int n = 1; n <= 5; n++) {
            if (n == 3) continue;
            if (n == 5) break;
            System.out.print(n + " ");
        }
    }
}
