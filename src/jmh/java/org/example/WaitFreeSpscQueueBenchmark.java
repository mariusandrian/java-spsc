package org.example;

/**
 * Measures TRUE end-to-end producer -> consumer handoff latency for {@link WaitFreeSpscQueue}.
 * See {@link AbstractSpscLatencyBenchmark} for the shared measurement approach/rationale.
 *
 * Caveat specific to WaitFreeSpscQueue: offer() returns false (rejects, does not overwrite)
 * when the queue is full, unlike SynchronizedSpsc. Under producer-faster-than-consumer
 * conditions this benchmark drops offers rather than overwriting unread data.
 */
public class WaitFreeSpscQueueBenchmark extends AbstractSpscLatencyBenchmark<WaitFreeSpscQueue<Long>> {

    @Override
    protected WaitFreeSpscQueue<Long> createQueue(int capacity) {
        return new WaitFreeSpscQueue<>(capacity);
    }

    @Override
    protected void offerValue(WaitFreeSpscQueue<Long> queue, long value) {
        queue.offer(value);
    }

    @Override
    protected Long pollValue(WaitFreeSpscQueue<Long> queue) {
        return queue.poll();
    }

    @Override
    protected String benchmarkLabel() {
        return "WaitFreeSpscQueue";
    }
}
