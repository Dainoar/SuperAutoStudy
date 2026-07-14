package com.tihai.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedPriorityBlockingQueueTest {

    @Test
    void enforcesCapacityWhileReturningHigherPriorityItemsFirst() {
        BoundedPriorityBlockingQueue<Integer> queue = new BoundedPriorityBlockingQueue<>(2);

        assertTrue(queue.offer(5));
        assertTrue(queue.offer(1));
        assertFalse(queue.offer(3));
        assertEquals(Integer.valueOf(1), queue.poll());
        assertTrue(queue.offer(3));
        assertEquals(Integer.valueOf(3), queue.poll());
        assertEquals(Integer.valueOf(5), queue.poll());
    }
}
