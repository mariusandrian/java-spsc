package org.example;

/**
 * Measures TRUE end-to-end producer -> consumer handoff latency for {@link SynchronizedSpsc}.
 * See {@link AbstractSpscLatencyBenchmark} for the shared measurement approach/rationale, and
 * {@link SyncronizedSpscBenchmark} for the superseded single-thread approach this replaces.
 *
 * Caveats specific to SynchronizedSpsc:
 *  - offer() never blocks: if the queue is full it silently overwrites the oldest unread slot.
 *    Under producer-faster-than-consumer conditions, some messages are dropped and never
 *    measured. This is expected/by design, not a bug in this benchmark - the histogram only
 *    reflects messages the consumer actually managed to read.
 *  - poll() never blocks: it returns null immediately when empty, so the consumer method
 *    busy-spins while waiting for data. This burns a full CPU core for the duration of every
 *    iteration (warmup included) by design - do NOT replace this with sleep()/park(), since
 *    that would add scheduler-wakeup latency into the measurement and corrupt the very thing
 *    we're trying to measure.
 */
public class SynchronizedSpscLatencyBenchmark extends AbstractSpscLatencyBenchmark<SynchronizedSpsc<Long>> {

    @Override
    protected SynchronizedSpsc<Long> createQueue(int capacity) {
        return new SynchronizedSpsc<>(capacity);
    }

    @Override
    protected void offerValue(SynchronizedSpsc<Long> queue, long value) {
        queue.offer(value);
    }

    @Override
    protected Long pollValue(SynchronizedSpsc<Long> queue) {
        return queue.poll();
    }

    @Override
    protected String benchmarkLabel() {
        return "SynchronizedSpsc";
    }
}
