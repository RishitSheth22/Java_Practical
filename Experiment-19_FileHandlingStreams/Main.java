import java.io.*;

public class Main {
    public static void main(String[] args) {
        String fileName = "sample.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Java file handling");
            writer.newLine();
            writer.write("Character and buffered streams");
        } catch (IOException e) {
            System.out.println("Write error: " + e.getMessage());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) System.out.println(line);
        } catch (IOException e) {
            System.out.println("Read error: " + e.getMessage());
        }

        // Byte stream example: copy the text file byte-by-byte.
        try (FileInputStream in = new FileInputStream(fileName);
             FileOutputStream out = new FileOutputStream("sample-copy.txt")) {
            int data;
            while ((data = in.read()) != -1) out.write(data);
            System.out.println("Byte-stream copy completed");
        } catch (IOException e) {
            System.out.println("Copy error: " + e.getMessage());
        }
    }
}
