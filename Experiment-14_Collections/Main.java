import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("Red");
        arrayList.add("Blue");
        System.out.println("ArrayList: " + arrayList);

        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(10);
        linkedList.addFirst(5);
        System.out.println("LinkedList: " + linkedList);

        Vector<String> vector = new Vector<>();
        vector.add("Java");
        vector.add("SQL");
        System.out.println("Vector: " + vector);

        HashSet<Integer> set = new HashSet<>();
        set.add(2);
        set.add(1);
        set.add(2);
        System.out.println("HashSet (no duplicates): " + set);
    }
}
