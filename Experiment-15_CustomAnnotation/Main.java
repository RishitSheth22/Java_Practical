import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PracticalInfo {
    String author();
    int experiment();
}

public class Main {
    @PracticalInfo(author = "Student", experiment = 15)
    public void display() {
        System.out.println("Custom annotation demo");
    }

    public static void main(String[] args) throws Exception {
        Main object = new Main();
        Method method = object.getClass().getMethod("display");
        PracticalInfo info = method.getAnnotation(PracticalInfo.class);

        System.out.println("Author: " + info.author());
        System.out.println("Experiment: " + info.experiment());
        object.display();
    }
}
