package com.tihai.manager;

import com.tihai.enums.WkTaskStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskLifecycleTest {

    @Test
    void recoversOnlyInterruptedTasksAndDoesNotRetryTerminalTasks() {
        assertTrue(TaskLifecycle.shouldRecover(WkTaskStatusEnum.QUEUE.getCode()));
        assertTrue(TaskLifecycle.shouldRecover(WkTaskStatusEnum.PROCESSING.getCode()));
        assertFalse(TaskLifecycle.shouldRecover(WkTaskStatusEnum.FINISHED.getCode()));
        assertFalse(TaskLifecycle.shouldRecover(WkTaskStatusEnum.ABNORMAL.getCode()));
        assertEquals(WkTaskStatusEnum.PENDING.getCode(), TaskLifecycle.recoveredStatus());
    }
}
