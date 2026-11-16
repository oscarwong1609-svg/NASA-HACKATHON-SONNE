package com.missionlab.ds;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * LINEAR STRUCTURE: FIFO queue (first in, first out), built from linked nodes.
 *
 *   head → [a] → [b] → [c] ← tail
 *   enqueue(d) links d after tail;  dequeue() removes a from head.
 *
 * Both operations are O(1), with no element shifting or array resizing.
 *
 * Mission Lab uses it for two things that happen in chronological order:
 * - the hangar test campaign (Session): tests run in the order they were scheduled;
 * - the flight timeline (Simulator → LaunchScreen): the simulator enqueues events
 *   in the order they happen and the launch screen dequeues and shows them in that same order.
 *
 * It can be iterated with for-each (front to back) without removing anything.
 */
public final class LinkedQueue<T> implements Iterable<T> {

    /** One link of the queue: a value and the next link (null if it is the last one). */
    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head; // next one out
    private Node<T> tail; // last one in
    private int size;

    public LinkedQueue() {}

    /** Independent copy: dequeuing from the copy does not change the original. */
    public LinkedQueue(LinkedQueue<T> other) {
        for (T value : other) enqueue(value);
    }

    /** Adds a value to the back of the queue. */
    public void enqueue(T value) {
        if (value == null) throw new IllegalArgumentException("The queue does not hold nulls");
        Node<T> n = new Node<>(value);
        if (tail == null) head = n;
        else tail.next = n;
        tail = n;
        size++;
    }

    /** Removes and returns the front of the queue. */
    public T dequeue() {
        if (head == null) throw new NoSuchElementException("The queue is empty");
        T value = head.value;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return value;
    }

    /** The front of the queue without removing it (or null if empty). */
    public T peek() {
        return head == null ? null : head.value;
    }

    /** Empties the queue. */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> at = head;

            @Override
            public boolean hasNext() {
                return at != null;
            }

            @Override
            public T next() {
                if (at == null) throw new NoSuchElementException();
                T v = at.value;
                at = at.next;
                return v;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Node<T> n = head; n != null; n = n.next) {
            sb.append(n.value);
            if (n.next != null) sb.append(", ");
        }
        return sb.append(']').toString();
    }
}
