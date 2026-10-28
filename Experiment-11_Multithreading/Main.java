class NumberThread extends Thread {
    @Override
    public void run() {
        for (int i = 1; i <= 3; i++) System.out.println("Thread class: " + i);
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        NumberThread first = new NumberThread();
        Thread second = new Thread(() -> {
            for (int i = 1; i <= 3; i++) System.out.println("Runnable: " + i);
        });

        first.start();
        second.start();
        first.join();
        second.join();
        System.out.println("Both threads finished");
    }
}
