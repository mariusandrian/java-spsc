import org.example.Queue;
import org.example.SynchronizedSpsc;

import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractSpscTest {
    void correctStringOrder(Queue<String> queue) {
        int dataCount = 50;

        Thread producer = new Thread(() -> {
            for (int i = 0; i < dataCount; i++) {
                String s = String.valueOf(i);
                while (!queue.offer(s)) {
                    Thread.onSpinWait();
                };
            }
        });

        var got  = new ArrayList<String>();
        Thread consumer = new Thread(() -> {
            String target = String.valueOf(dataCount-1);
            String s = null;
            while (!Objects.equals(s, target)) {
                s = queue.poll();
                if (s != null) {
                    got.add(s);
                }
            }
        });

        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert monotonically increasing.
        int prev = -1;
        for (String s : got) {
            int cur = Integer.parseInt(s);
            assertTrue(cur > prev, "cur is ");
            prev = cur;
        }

        System.out.println("got size: " + got.size());

    }

}
