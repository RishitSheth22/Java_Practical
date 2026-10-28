public class Main {
    public static void main(String[] args) {
        int age = 20;
        double price = 99.75;
        char grade = 'A';
        boolean passed = true;

        int wholeNumber = (int) price;       // narrowing cast
        double decimalNumber = age;          // widening conversion
        String text = "123";
        int parsedNumber = Integer.parseInt(text);

        System.out.println("Age: " + age);
        System.out.println("Price: " + price);
        System.out.println("Grade: " + grade);
        System.out.println("Passed: " + passed);
        System.out.println("Double to int: " + wholeNumber);
        System.out.println("Int to double: " + decimalNumber);
        System.out.println("String to int: " + parsedNumber);
    }
}
