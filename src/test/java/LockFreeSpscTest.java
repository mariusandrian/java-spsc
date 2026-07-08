import org.example.LockFreeSpsc;
import org.example.SynchronizedSpsc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LockFreeSpscTest extends AbstractSpscTest {
    @Test
    public void correctStringOrder() {
        super.correctStringOrder(new LockFreeSpsc<>(30));
    }

    @Test
    public void capacityOneIsUsable() {
        var queue = new LockFreeSpsc<String>(1);
        assertTrue(queue.offer("a"));
        assertFalse(queue.offer("b"));
        assertEquals("a", queue.poll());
        assertNull(queue.poll());
        assertTrue(queue.offer("c"));
        assertEquals("c", queue.poll());
    }

    @Test
    public void holdsExactlyCapacityElements() {
        int capacity = 5;
        var queue = new LockFreeSpsc<Integer>(capacity);
        for (int i = 0; i < capacity; i++) {
            assertTrue(queue.offer(i), "offer " + i + " should succeed");
        }
        assertFalse(queue.offer(capacity), "queue should be full");
        for (int i = 0; i < capacity; i++) {
            assertEquals(i, queue.poll());
        }
        assertNull(queue.poll());
    }

    @Test
    public void wrapsAroundManyTimes() {
        var queue = new LockFreeSpsc<Integer>(3);
        for (int i = 0; i < 1000; i++) {
            assertTrue(queue.offer(i));
            assertEquals(i, queue.poll());
        }
        assertNull(queue.poll());
    }
}
