import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int[] numbers = { 5, 2, 8, 1, 3 };
        Arrays.sort(numbers);
        System.out.println("Sorted 1D array: " + Arrays.toString(numbers));

        int[][] matrix = { { 9, 4 }, { 7, 2 } };
        for (int[] row : matrix) {
            Arrays.sort(row);
        }
        System.out.println("Rows sorted in 2D array:");
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }
}
