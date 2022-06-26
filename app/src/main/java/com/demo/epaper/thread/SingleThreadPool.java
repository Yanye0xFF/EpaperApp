package com.demo.epaper.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadPool {

    private static SingleThreadPool threadPool;
    private ExecutorService fixedThreadPool;

    private static int referenceCount = 0;

    public static SingleThreadPool getInstance() {
        if(threadPool == null) {
            synchronized(SingleThreadPool.class) {
                if(threadPool == null) {
                    threadPool = new SingleThreadPool();
                }
            }
        }
        referenceCount++;
        return threadPool;
    }

    public SingleThreadPool() {
        fixedThreadPool = Executors.newSingleThreadExecutor();
    }

    public void execute(Runnable runnable) {
        fixedThreadPool.execute(runnable);
    }

    public void close() {
        referenceCount--;
        if(referenceCount == 0) {
            fixedThreadPool.shutdown();
            fixedThreadPool = null;
            threadPool = null;
        }
    }

}


