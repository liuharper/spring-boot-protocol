package com.github.netty.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Collector (can control the number of instances, ensure the stability of instances, no explosion, no explosion, reduce the number of gc)
 * @author wangzihao
 */
public class Recycler<T> {
    private static final int DEFAULT_INSTANCE_COUNT = SystemPropertyUtil.getInt("netty-core.recyclerCount",30);
    private static final boolean ENABLE = SystemPropertyUtil.getBoolean("netty-core.recyclerEnable",true);
    /**
     * The instance queue of the current object
     */
    private Queue<T> queue;
    /**
     * New instance factory for the current object
     */
    private Supplier<T> supplier;

    /**
     * All recyclers
     */
    private static final List<Recycler> RECYCLER_LIST = new ArrayList<>();
    public static final AtomicInteger TOTAL_COUNT = new AtomicInteger();
    public static final AtomicInteger HIT_COUNT = new AtomicInteger();

    private StackTraceElement formStack;
    private Thread formThread;

    public Recycler(Supplier<T> supplier) {
        this(DEFAULT_INSTANCE_COUNT,supplier);
    }

    public Recycler(int instanceCount, Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
        this.queue = new Queue<>();
        RECYCLER_LIST.add(this);
        this.formThread = Thread.currentThread();
        this.formStack = formThread.getStackTrace()[3];

        if(ENABLE) {
            for (int i = 0; i < instanceCount; i++) {
                recycleInstance(supplier.get());
            }
        }
    }

    /**
     * Gets a list of all recyclers
     * @return List
     */
    public static List<Recycler> getRecyclerList() {
        return RECYCLER_LIST;
    }

    /**
     * Get an instance
     * @return object
     */
    public T getInstance() {
        if(ENABLE) {
            TOTAL_COUNT.incrementAndGet();
            T value = queue.pop();
            if (value == null) {
                value = supplier.get();
            } else {
                HIT_COUNT.incrementAndGet();
            }
            return value;
        }else {
            return supplier.get();
        }
    }

    /**
     * Recycling instance
     * @param value value
     */
    public void recycleInstance(T value) {
        queue.push(value);
    }


    @Override
    public String toString() {
        return "Recycler{" +
                "size=" + queue.size() +
                ", formStack=" + StringUtil.simpleClassName(formStack.getClassName()) +
                ", formThread=" + formThread +
                '}';
    }

    /**
     * Queue of instances
     * @param <E> type
     */
    private static class Queue<E> extends ConcurrentLinkedDeque<E> {
         @Override
         public void push(E e) {
             super.addLast(e);
         }

         @Override
         public E pop() {
             return super.pollFirst();
         }
     }
}
