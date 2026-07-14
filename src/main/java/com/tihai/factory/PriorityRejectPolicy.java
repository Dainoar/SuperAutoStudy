package com.tihai.factory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Rejects explicitly so the task lifecycle can return the task to PENDING.
 */
public class PriorityRejectPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        throw new RejectedExecutionException("任务队列已满或线程池已关闭");
    }
}
