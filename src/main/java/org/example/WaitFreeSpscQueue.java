package org.example;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class WaitFreeSpscQueue<E> {
        private final E[] buffer;
        private final int mask;

        // Volatile indices to establish cross-thread visibility
        private volatile long head = 0;
        private volatile long tail = 0;

        // VarHandle VarReferences for atomic/ordered memory operations
        private static final VarHandle HEAD_HANDLE;
        private static final VarHandle TAIL_HANDLE;
        private static final VarHandle ARRAY_HANDLE;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                HEAD_HANDLE = l.findVarHandle(WaitFreeSpscQueue.class, "head", long.class);
                TAIL_HANDLE = l.findVarHandle(WaitFreeSpscQueue.class, "tail", long.class);
                ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(Object[].class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unchecked")
        public WaitFreeSpscQueue(int capacity) {
            // Capacity must be a power of two for the fast bitwise modulo mask
            int powerOfTwoCap = findNextPowerOfTwo(capacity);
            this.buffer = (E[]) new Object[powerOfTwoCap];
            this.mask = powerOfTwoCap - 1;
        }

        /**
         * Inserts an element. Called ONLY by the producer thread.
         * Returns true if successful, false if the queue is full.
         */
        public boolean offer(E element) {
            if (element == null) throw new NullPointerException();

            long currentTail = (long) TAIL_HANDLE.getVolatile(this);
            long currentHead = (long) HEAD_HANDLE.getAcquire(this); // Cheap read

            // Check if queue is full
            if (currentTail - currentHead >= buffer.length) {
                return false;
            }

            int index = (int) (currentTail & mask);
            // Store the element safely away from the consumer's eyes initially
            ARRAY_HANDLE.setRelease(buffer, index, element);

            // Publish the element by advancing the tail (Release fence ensures element visibility)
            TAIL_HANDLE.setRelease(this, currentTail + 1);
            return true;
        }

        /**
         * Extracts an element. Called ONLY by the consumer thread.
         * Returns the element, or null if the queue is empty.
         */
        @SuppressWarnings("unchecked")
        public E poll() {
            long currentHead = (long) HEAD_HANDLE.getVolatile(this);
            long currentTail = (long) TAIL_HANDLE.getAcquire(this); // Cheap read

            // Check if queue is empty
            if (currentHead == currentTail) {
                return null;
            }

            int index = (int) (currentHead & mask);
            // Grab the element safely using Acquire semantics
            E element = (E) ARRAY_HANDLE.getAcquire(buffer, index);

            // Clear the slot to prevent memory leaks
            ARRAY_HANDLE.setRelease(buffer, index, null);

            // Make the slot available again by advancing the head
            HEAD_HANDLE.setRelease(this, currentHead + 1);
            return element;
        }

        private int findNextPowerOfTwo(int value) {
            return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
        }
}
