package com.tihai.manager;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Copyright : DuanInnovator
 * @Description :回滚管理器
 * @Author : DuanInnovator
 * @CreateTime : 2025/2/27
 * @Link : <a href="https://github.com/DuanInnovator/SuperAutotudy">...</a>
 **/
@Component
public class RollBackManager {
    private final ConcurrentHashMap<String, AtomicInteger> rollbackCounts = new ConcurrentHashMap<>();

    public void addTimes(String id) throws Exception {
        int attempts = rollbackCounts.computeIfAbsent(id, key -> new AtomicInteger()).incrementAndGet();
        if (attempts > 3) {
            rollbackCounts.remove(id);
            throw new RuntimeException("回滚次数已达3次，请手动检查学习通任务点完成情况");
        }
    }
}

