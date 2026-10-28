import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) {
        Path directory = Path.of("nio-demo");
        Path file = directory.resolve("notes.txt");

        try {
            Files.createDirectories(directory);
            Files.writeString(file, "Created using Java NIO.\n");
            Files.writeString(file, "Second line.\n", StandardOpenOption.APPEND);

            System.out.println("File exists: " + Files.exists(file));
            System.out.println("Content:");
            System.out.println(Files.readString(file));

            Path copy = directory.resolve("notes-copy.txt");
            Files.copy(file, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied to: " + copy.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("NIO error: " + e.getMessage());
        }
    }
}
