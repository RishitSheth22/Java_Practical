class Animal {
    String name = "Animal";

    void sound() {
        System.out.println("Animal makes a sound");
    }
}

class Dog extends Animal {
    String name = "Dog";

    @Override
    void sound() {
        super.sound();
        System.out.println("Dog barks");
        System.out.println("Parent name: " + super.name);
    }
}

public class Main {
    public static void main(String[] args) {
        Dog dog = new Dog();
        dog.sound();
    }
}
