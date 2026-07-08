import org.example.SynchronizedSpsc;
import org.junit.jupiter.api.Test;


public class SynchronizedSpscTest extends AbstractSpscTest {
    @Test
    public void correctStringOrder() {
        super.correctStringOrder(new SynchronizedSpsc<>(30));
    }
}
