package org.example;

import java.util.concurrent.atomic.AtomicLong;

public class LockFreeSpsc<E> implements Queue<E> {
    private final Object[] array;
    private final int capacity;
    // Monotonically increasing, never wrapped: with long cursors overflow is
    // unreachable in practice, so `ptr % capacity` never goes negative.
    // Invariant: readPtr is written only by the consumer thread, writePtr only
    // by the producer thread — that is what makes acquire/release sufficient.
    private final AtomicLong readPtr = new AtomicLong(0);
    private final AtomicLong writePtr = new AtomicLong(0);

    public LockFreeSpsc(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1");
        this.capacity = capacity;
        array = new Object[capacity];
    }

    public boolean offer(E val) {
        long localWritePtr = writePtr.getPlain(); // own cursor: plain read, we are its only writer
        long localReadPtr = readPtr.getAcquire();
        if (localWritePtr - localReadPtr == capacity) {
            return false;
        }
        array[(int) (localWritePtr % capacity)] = val;
        writePtr.setRelease(localWritePtr + 1); // publishes the array store to the consumer
        return true;
    }

    public E poll() {
        long localReadPtr = readPtr.getPlain(); // own cursor: plain read, we are its only writer
        long localWritePtr = writePtr.getAcquire();
        if (localReadPtr == localWritePtr) {
            return null;
        }
        @SuppressWarnings("unchecked") E val = (E) array[(int) (localReadPtr % capacity)];
        readPtr.setRelease(localReadPtr + 1); // frees the slot only after the read above
        return val;
    }
}
