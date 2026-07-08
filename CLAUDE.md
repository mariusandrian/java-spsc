# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build tool: Gradle (via wrapper — always use `./gradlew`, not a system-installed `gradle`). JDK 26.

```bash
./gradlew build              # compile + test + jmh compile
./gradlew test                # run JUnit 5 tests
./gradlew test --tests SynchronizedSpscTest   # run a single test class
./gradlew jmh                 # compile and run ALL JMH benchmarks (src/jmh/java)
./gradlew jmhJar              # build a standalone runnable JMH jar
```

To run a single JMH benchmark class (or pass native JMH flags like fork/warmup/iteration counts), build the jar once and invoke it directly with the JMH CLI, filtering by class name regex:

```bash
./gradlew jmhJar
java -jar build/libs/java-spsc-1.0-SNAPSHOT-jmh.jar <BenchmarkClassName> [-f 1] [-wi 3] [-i 5]
```

There is no linter/formatter configured in this project.

## Architecture

This repo is a sandbox for building and benchmarking SPSC (single-producer/single-consumer) bounded queue implementations, comparing a simple lock-based version against lock-free variants.

**Source layout** (note: gradle standard layout, `me.champeau.jmh` plugin adds a fourth `jmh` source set alongside `main`/`test`):
- `src/main/java/org/example/` — the queue implementations themselves.
- `src/jmh/java/org/example/` — JMH benchmark classes (`jmhImplementation` dependencies, e.g. HdrHistogram, are only available here, not in `main`).
- `src/test/java/` — JUnit 5 tests (default/no package — note `SynchronizedSpscTest` has no `package` declaration, unlike everything else which uses `org.example`).

**Queue implementations**, both bounded ring buffers over an `Object[]`/`E[]` backing array with `readPtr`/`writePtr` (or `head`/`tail`) cursors, where the producer silently **overwrites the oldest unread element** once the buffer is full (never blocks, never rejects):
- `SynchronizedSpsc<E>` — the original, `offer()`/`poll()` guarded by `synchronized`. Despite the class name, note that a lock-free rewrite of this same class (keeping the class name and `void offer(E)`/`E poll()` signatures for compatibility) has been planned but not yet applied as of the last check — verify current state before assuming it's still lock-based.
- `WaitFreeSpscQueue<E>` — a lock-free/wait-free rewrite using `VarHandle`s (`getAcquire`/`setRelease`) on `head`/`tail` fields instead of `synchronized`. Requires **power-of-two capacity** (constructor rounds up via `findNextPowerOfTwo`) so indexing can use a bitmask (`& mask`) instead of `%`. `offer()` returns `boolean` (rejects when full, does **not** overwrite) — this is a behavioral difference from `SynchronizedSpsc`, not just an implementation detail; check which contract a caller actually needs.

When editing either queue for concurrency correctness, the critical invariant is that each cursor field must have exactly one writer thread (`writePtr`/`tail` from the producer only, `readPtr`/`head` from the consumer only) — this is what allows plain `volatile`/`VarHandle` acquire-release instead of CAS. If a change reintroduces cross-thread writes to the same cursor (e.g. to restore overwrite-on-full semantics in a lock-free queue), it reopens a real data race on the array slot being overwritten while concurrently read, which needs explicit handling (lap-detection + retry, or per-slot sequence numbers) — don't add "silent overwrite when full" to a lock-free queue without that.

**Benchmarking approach**: JMH's own per-invocation timer cannot correlate a specific `offer()` call with the `poll()` call that eventually retrieves that same element across two different threads, so the benchmarks here don't rely on JMH's own timing for cross-thread latency. Instead (see `SynchronizedSpscLatencyBenchmark` / `WaitFreeSpscQueueBenchmark`), the producer stamps `System.nanoTime()` as the payload itself, and the consumer computes `System.nanoTime() - payload` at the moment of a successful `poll()`, recording each sample into a shared `HdrHistogram` (`AtomicHistogram`, since only the consumer writes but `@TearDown` may run on a different thread). Percentiles are printed manually in `@TearDown(Level.Iteration)`, filtered to `IterationType.MEASUREMENT` only (via injected `IterationParams`) so warmup iterations don't pollute output. Both benchmark classes use `@State(Scope.Group)` + `@Group`/`@GroupThreads(1)` to pin one real producer thread and one real consumer thread per queue instance. The consumer's spin-wait loop must check `Control.stopMeasurement` in addition to "got a value" — without it, the loop can hang forever at the tail of an iteration once the producer's own iteration has already ended and the queue stays empty (JMH cannot preempt mid-invocation).
