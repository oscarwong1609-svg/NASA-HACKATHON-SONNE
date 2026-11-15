package com.missionlab.ds;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ESTRUCTURA LINEAL: cola FIFO (el primero que entra es el primero que sale), hecha con nodos enlazados.
 *
 *   head → [a] → [b] → [c] ← tail
 *   enqueue(d) engancha d detrás de tail;  dequeue() saca a de head.
 *
 * Las dos operaciones son O(1) y no hay que mover elementos ni cambiar el tamaño de un array.
 *
 * En Mission Lab se usa para dos cosas que ocurren en orden cronológico:
 * - la campaña de pruebas del hangar (Session): las pruebas salen en el orden en que se programaron;
 * - la línea de tiempo del vuelo (Simulator → LaunchScreen): el simulador encola los sucesos
 *   en el orden en que pasan y la pantalla de lanzamiento los saca y los muestra en ese mismo orden.
 *
 * Se puede recorrer con for-each (de delante hacia atrás) sin sacar nada.
 */
public final class LinkedQueue<T> implements Iterable<T> {

    /** Un eslabón de la cola: un valor y el siguiente eslabón (null si es el último). */
    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head; // el próximo en salir
    private Node<T> tail; // el último que entró
    private int size;

    public LinkedQueue() {}

    /** Copia independiente: sacar elementos de la copia no cambia la original. */
    public LinkedQueue(LinkedQueue<T> other) {
        for (T value : other) enqueue(value);
    }

    /** Pone un valor al final de la cola. */
    public void enqueue(T value) {
        if (value == null) throw new IllegalArgumentException("The queue does not hold nulls");
        Node<T> n = new Node<>(value);
        if (tail == null) head = n;
        else tail.next = n;
        tail = n;
        size++;
    }

    /** Saca y devuelve el primero de la cola. */
    public T dequeue() {
        if (head == null) throw new NoSuchElementException("The queue is empty");
        T value = head.value;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return value;
    }

    /** El primero de la cola, sin sacarlo (o null si está vacía). */
    public T peek() {
        return head == null ? null : head.value;
    }

    /** Vacía la cola. */
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
