import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        String email = "student@example.com";
        Pattern pattern = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
        Matcher matcher = pattern.matcher(email);

        System.out.println("Email: " + email);
        System.out.println("Valid format: " + matcher.matches());

        Pattern digits = Pattern.compile("\\d+");
        Matcher numbers = digits.matcher("Room 204, floor 3");
        while (numbers.find()) {
            System.out.println("Number found: " + numbers.group());
        }
    }
}
