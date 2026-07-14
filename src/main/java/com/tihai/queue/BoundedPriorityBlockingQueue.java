package com.tihai.queue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A priority queue with a real capacity. {@link PriorityBlockingQueue} alone
 * is unbounded, so it cannot trigger a ThreadPoolExecutor rejection policy.
 */
public class BoundedPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final PriorityBlockingQueue<E> delegate = new PriorityBlockingQueue<>();
    private final Semaphore slots;

    public BoundedPriorityBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.slots = new Semaphore(capacity);
    }

    @Override
    public boolean offer(E element) {
        if (!slots.tryAcquire()) {
            return false;
        }
        boolean added = false;
        try {
            added = delegate.offer(element);
            return added;
        } finally {
            if (!added) {
                slots.release();
            }
        }
    }

    @Override
    public void put(E element) throws InterruptedException {
        slots.acquire();
        boolean added = false;
        try {
            added = delegate.offer(element);
        } finally {
            if (!added) {
                slots.release();
            }
        }
    }

    @Override
    public boolean offer(E element, long timeout, TimeUnit unit) throws InterruptedException {
        if (!slots.tryAcquire(timeout, unit)) {
            return false;
        }
        boolean added = false;
        try {
            added = delegate.offer(element);
            return added;
        } finally {
            if (!added) {
                slots.release();
            }
        }
    }

    @Override
    public E poll() {
        E element = delegate.poll();
        releaseSlot(element);
        return element;
    }

    @Override
    public E take() throws InterruptedException {
        E element = delegate.take();
        releaseSlot(element);
        return element;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E element = delegate.poll(timeout, unit);
        releaseSlot(element);
        return element;
    }

    @Override
    public E peek() {
        return delegate.peek();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public int remainingCapacity() {
        return slots.availablePermits();
    }

    @Override
    public boolean remove(Object element) {
        boolean removed = delegate.remove(element);
        if (removed) {
            slots.release();
        }
        return removed;
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        int drained = delegate.drainTo(collection);
        slots.release(drained);
        return drained;
    }

    @Override
    public int drainTo(Collection<? super E> collection, int maxElements) {
        int drained = delegate.drainTo(collection, maxElements);
        slots.release(drained);
        return drained;
    }

    @Override
    public void clear() {
        int count = delegate.size();
        delegate.clear();
        slots.release(count);
    }

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> iterator = delegate.iterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
                slots.release();
            }
        };
    }

    private void releaseSlot(E element) {
        if (element != null) {
            slots.release();
        }
    }
}
