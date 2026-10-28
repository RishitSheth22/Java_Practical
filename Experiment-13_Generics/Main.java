class Box<T> {
    private T value;

    void set(T value) { this.value = value; }
    T get() { return value; }
}

public class Main {
    static <T> void printArray(T[] items) {
        for (T item : items) System.out.print(item + " ");
        System.out.println();
    }

    public static void main(String[] args) {
        Box<String> textBox = new Box<>();
        textBox.set("Hello Generics");
        System.out.println(textBox.get());

        Integer[] numbers = { 1, 2, 3 };
        printArray(numbers);
    }
}
