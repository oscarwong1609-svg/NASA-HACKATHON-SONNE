package com.missionlab.ds;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedQueueTest {

    @Test
    void firstInFirstOut() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("liftoff");
        q.enqueue("orbit");
        q.enqueue("contact");
        assertEquals(3, q.size());
        assertEquals("liftoff", q.peek());
        assertEquals("liftoff", q.dequeue());
        assertEquals("orbit", q.dequeue());
        q.enqueue("photo"); // enters at the back even after something has been dequeued
        assertEquals("contact", q.dequeue());
        assertEquals("photo", q.dequeue());
        assertTrue(q.isEmpty());
        assertNull(q.peek());
    }

    @Test
    void emptyQueueRefusesToDequeue() {
        LinkedQueue<Integer> q = new LinkedQueue<>();
        assertThrows(NoSuchElementException.class, q::dequeue);
        q.enqueue(1);
        q.dequeue();
        assertThrows(NoSuchElementException.class, q::dequeue);
        q.enqueue(2); // tail was reset correctly
        int v = q.dequeue();
        assertEquals(2, v);
    }

    @Test
    void nullsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LinkedQueue<String>().enqueue(null));
    }

    @Test
    void copyIsIndependentAndIterationKeepsOrder() {
        LinkedQueue<Integer> q = new LinkedQueue<>();
        for (int i = 0; i < 5; i++) q.enqueue(i);
        LinkedQueue<Integer> copy = new LinkedQueue<>(q);
        copy.dequeue();
        copy.enqueue(99);
        List<Integer> seen = new ArrayList<>();
        for (int v : q) seen.add(v);
        assertEquals(List.of(0, 1, 2, 3, 4), seen);
        assertEquals(5, q.size());
        assertEquals("[1, 2, 3, 4, 99]", copy.toString());
    }

    @Test
    void clearEmptiesTheQueue() {
        LinkedQueue<Integer> q = new LinkedQueue<>();
        q.enqueue(1);
        q.enqueue(2);
        q.clear();
        assertTrue(q.isEmpty());
        q.enqueue(3);
        int v = q.dequeue();
        assertEquals(3, v);
    }
}
