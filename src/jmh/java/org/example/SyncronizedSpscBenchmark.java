package org.example;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class SyncronizedSpscBenchmark {
    private SynchronizedSpsc<String> queue;

    @Setup
    public void setup() {
        queue = new SynchronizedSpsc<>(64);
    }

//    @Benchmark
    public String testQueueLatency() {
        queue.offer(String.valueOf(System.nanoTime()));
        return queue.poll();
    }
}
