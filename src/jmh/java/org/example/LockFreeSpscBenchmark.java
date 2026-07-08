package org.example;

public class LockFreeSpscBenchmark extends AbstractSpscLatencyBenchmark<LockFreeSpsc<Long>> {
    @Override
    protected LockFreeSpsc<Long> createQueue(int capacity) {
        return new LockFreeSpsc<>(capacity);
    }

    @Override
    protected void offerValue(LockFreeSpsc<Long> queue, long value) {
        queue.offer(value);
    }

    @Override
    protected Long pollValue(LockFreeSpsc<Long> queue) {
        return queue.poll();
    }

    @Override
    protected String benchmarkLabel() {
        return "LockFreeSpscQueue";
    }
}
