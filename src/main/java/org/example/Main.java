package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        SynchronizedSpsc<Integer> ss = new SynchronizedSpsc<>(1024);
        for (int i = 0; i < 1_000_000; i++) {
            ss.offer(i);
        }
        IO.println("done");
    }
}
