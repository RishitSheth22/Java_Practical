public class Main {
    public static void main(String[] args) {
        String text = "Java Programming";
        System.out.println("Length: " + text.length());
        System.out.println("Uppercase: " + text.toUpperCase());
        System.out.println("Substring: " + text.substring(5));
        System.out.println("Contains Java: " + text.contains("Java"));
        System.out.println("Replace: " + text.replace("Java", "Core Java"));

        StringBuilder builder = new StringBuilder("Java");
        builder.append(" Practical");
        builder.insert(0, "My ");
        builder.replace(0, 2, "Our");
        System.out.println("StringBuilder: " + builder);
        System.out.println("Reversed: " + builder.reverse());
    }
}
