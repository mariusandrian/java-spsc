./gradlew jmhJar
java -jar build/libs/java-spsc-1.0-SNAPSHOT-jmh.jar SynchronizedSpscLatencyBenchmark

./gradlew compileJava
java "-Xlog:gc:stdout:time,uptime" -cp build/classes/java/main org.example.Main