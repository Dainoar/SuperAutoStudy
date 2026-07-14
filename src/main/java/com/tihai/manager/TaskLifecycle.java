package com.tihai.manager;

import com.tihai.enums.WkTaskStatusEnum;

/**
 * Defines the persisted task states that can safely be recovered after a
 * process restart.
 */
public final class TaskLifecycle {

    private TaskLifecycle() {
    }

    public static boolean shouldRecover(Integer status) {
        return WkTaskStatusEnum.QUEUE.getCode().equals(status)
                || WkTaskStatusEnum.PROCESSING.getCode().equals(status);
    }

    public static Integer recoveredStatus() {
        return WkTaskStatusEnum.PENDING.getCode();
    }
}
