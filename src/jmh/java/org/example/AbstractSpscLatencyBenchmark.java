package org.example;

import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.SingleWriterRecorder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;

import java.util.concurrent.TimeUnit;

/**
 * Shared scaffolding for measuring TRUE end-to-end producer -> consumer handoff latency of an
 * SPSC queue, using one dedicated producer thread and one dedicated consumer thread running
 * concurrently (via JMH's @Group/@GroupThreads).
 *
 * JMH's own timing of the two @Benchmark methods below is NOT the metric we care about here.
 * The real metric is a HdrHistogram of (System.nanoTime() at poll time) - (System.nanoTime()
 * at offer time), built and printed ourselves. @BenchmarkMode is therefore just a nominal
 * driver mode.
 *
 * Subclasses supply the queue-specific glue (construction, how to call offer/poll, and a label
 * for the printed report) via the abstract hook methods below; add a queue-specific caveats
 * section to each subclass's own Javadoc (e.g. whether offer() blocks, rejects, or overwrites
 * on a full queue).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Group)
public abstract class AbstractSpscLatencyBenchmark<Q> {

    private static final int DEFAULT_QUEUE_CAPACITY = 64;
    private static final long HIGHEST_TRACKABLE_LATENCY_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final int HISTOGRAM_SIGNIFICANT_DIGITS = 3;
    private static final long INTER_MESSAGE_NANOS = 1_000;

    private Q queue;
    private long nextSendNanos;

    // Only the consumer thread ever calls recordValue(); @TearDown(Level.Iteration) on a
    // Scope.Group state may be invoked by an arbitrary thread rather than the consumer's own
    // (JMH's public API doesn't guarantee which). AtomicHistogram (rather than the plain,
    // non-thread-safe Histogram) is used so that reads in @TearDown are guaranteed to see all
    // values recorded by the consumer thread, without relying on unspecified internal JMH
    // synchronization behavior.
    private SingleWriterRecorder histogram;

    protected abstract Q createQueue(int capacity);

    protected abstract void offerValue(Q queue, long value);

    protected abstract Long pollValue(Q queue);

    protected abstract String benchmarkLabel();

    /** Override only if a particular queue implementation needs a non-default capacity. */
    protected int queueCapacity() {
        return DEFAULT_QUEUE_CAPACITY;
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        queue = createQueue(queueCapacity());
        // AtomicHistogram doesn't support setAutoResize(); a fixed 60s ceiling is already far
        // larger than any latency this benchmark should ever see, so no resize is needed.
        histogram = new SingleWriterRecorder(HIGHEST_TRACKABLE_LATENCY_NANOS, HISTOGRAM_SIGNIFICANT_DIGITS);
        nextSendNanos = System.nanoTime() + INTER_MESSAGE_NANOS;
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration(IterationParams iterationParams) {
        // Only print measurement iterations - warmup iterations exist purely to let the
        // JIT/queue reach steady state and are not meaningful latency data.
        if (iterationParams.getType() != IterationType.MEASUREMENT) {
            return;
        }
        Histogram histogram = this.histogram.getIntervalHistogram();
        System.out.println();
        System.out.println("=== " + benchmarkLabel() + " end-to-end offer->poll latency (measurement iteration) ===");
        System.out.printf("  samples = %d%n", histogram.getTotalCount());
        System.out.printf("  mean    = %.0f ns%n", histogram.getMean());
        System.out.printf("  p50     = %d ns%n", histogram.getValueAtPercentile(50));
        System.out.printf("  p90     = %d ns%n", histogram.getValueAtPercentile(90));
        System.out.printf("  p99     = %d ns%n", histogram.getValueAtPercentile(99));
        System.out.printf("  p99.9   = %d ns%n", histogram.getValueAtPercentile(99.9));
        System.out.printf("  max     = %d ns%n", histogram.getMaxValue());
    }

    @Benchmark
    @Group("pipeline")
    @GroupThreads(1)
    public void produce() {
        long scheduled = nextSendNanos;
        while (System.nanoTime() < scheduled) {
            Thread.onSpinWait();
        }
        offerValue(queue, scheduled);
        nextSendNanos = System.nanoTime() + INTER_MESSAGE_NANOS;
    }

    @Benchmark
    @Group("pipeline")
    @GroupThreads(1)
    public void consume(Control control) {
        // control.stopMeasurement is essential here: without it, this spin loop can hang
        // forever at the tail of an iteration if the producer's own iteration already ended
        // and the queue is empty - JMH cannot preempt us mid-invocation, only this thread can
        // decide to bail out.
        Long sentAtNanos = pollValue(queue);
        while (sentAtNanos == null && !control.stopMeasurement) {
            sentAtNanos = pollValue(queue);
        }
        if (sentAtNanos == null) {
            // Iteration ended while we were waiting for the next message; nothing to record.
            return;
        }
        long latencyNanos = System.nanoTime() - sentAtNanos;
        if (latencyNanos < 0) {
            // Defensive only; System.nanoTime() differences should never be negative here,
            // but HdrHistogram rejects negative values.
            latencyNanos = 0;
        }
        histogram.recordValue(latencyNanos);
    }
}
