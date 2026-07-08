package org.example;

public interface Queue<E> {
    boolean offer(E e);
    E poll();
}
