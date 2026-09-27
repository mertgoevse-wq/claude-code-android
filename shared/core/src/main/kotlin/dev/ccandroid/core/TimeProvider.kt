package dev.ccandroid.core

public interface TimeProvider {
    public fun currentTimeMillis(): Long
    public fun nanoTime(): Long
}

public class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun nanoTime(): Long = System.nanoTime()
}
