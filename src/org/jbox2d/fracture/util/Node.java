package org.jbox2d.fracture.util;

/**
 * Obecny uzol spojoveho zoznamu
 *
 * @author Marek Benovic
 * @param <T>
 */
public class Node<T> {
    /**
     * Hodnota.
     */
    public final T value;
    
    /**
     * Susedne prvky spojoveho zoznamu.
     */
    public Node<T> next, prev;

    /**
     * Inicializuje uzol.
     * @param value 
     */
    public Node(T value){
        this.value = value;
    }
    
    /**
     * Inicializuje vrchol a nastavi referencie na susedne prvky.
     * @param p
     * @param left
     * @param right 
     */
    public Node(T p, Node<T> left, Node<T> right) {
        this(p);
        this.next = left;
        this.prev = right;
    }
}