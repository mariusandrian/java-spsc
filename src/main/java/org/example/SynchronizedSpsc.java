package org.example;

public class SynchronizedSpsc<E> implements Queue<E> {
    private final Object[] array;
    private final int capacity;
    private int readPtr = 0;
    private int writePtr = 0;

    public SynchronizedSpsc(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1");
        this.capacity = capacity;
        array = new Object[capacity];
    }

    public synchronized boolean offer(E val) {
        if ((writePtr+1) % capacity == (readPtr % capacity)) {
            return false;
        }
//        System.out.println("offer");
        array[writePtr % capacity] = val;
//        System.out.println("offer written to " + writePtr % capacity);
//        System.out.println(writePtr);
        if (writePtr - readPtr == capacity) {
            readPtr++;
        }
        writePtr++;
        return true;
//        System.out.println("offer writePtr: " + writePtr);
//        System.out.println("offer readPtr: " + readPtr);
    }

    public synchronized E poll() {
//        System.out.println("poll");
        if (readPtr == writePtr) {
            return null;
        }
        @SuppressWarnings("unchecked") E val = (E) array[readPtr % capacity];
//        System.out.println("poll readPtr: " + readPtr % capacity);
//        System.out.println("val is " + val);

        readPtr++;
        return val;
    }
}
