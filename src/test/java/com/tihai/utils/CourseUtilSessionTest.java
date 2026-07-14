package com.tihai.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseUtilSessionTest {

    @Test
    void keepsCookieStateIsolatedBetweenConcurrentTasks() throws Exception {
        CourseUtil courseUtil = new CourseUtil();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstSessionReady = new CountDownLatch(1);
        CountDownLatch secondSessionReady = new CountDownLatch(1);

        try {
            Future<String> firstTask = executor.submit(() -> {
                courseUtil.setAccount("first-account");
                courseUtil.setCookies("_uid=first-user");
                firstSessionReady.countDown();
                assertTrue(secondSessionReady.await(2, TimeUnit.SECONDS));
                return courseUtil.getValue("_uid");
            });
            Future<String> secondTask = executor.submit(() -> {
                assertTrue(firstSessionReady.await(2, TimeUnit.SECONDS));
                courseUtil.setAccount("second-account");
                courseUtil.setCookies("_uid=second-user");
                secondSessionReady.countDown();
                return courseUtil.getValue("_uid");
            });

            assertEquals("first-user", firstTask.get(2, TimeUnit.SECONDS));
            assertEquals("second-user", secondTask.get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
