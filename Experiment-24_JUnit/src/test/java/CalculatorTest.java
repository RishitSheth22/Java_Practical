import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    @Test
    void testAddition() {
        Calculator calculator = new Calculator();
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    void testMultiplication() {
        Calculator calculator = new Calculator();
        assertEquals(12, calculator.multiply(3, 4));
    }
}
