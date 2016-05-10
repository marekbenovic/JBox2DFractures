package org.jbox2d.fracture.voronoi;

/**
 * Uzol spojoveho zoznamu pre konvexny obal.
 *
 * @author Marek Benovic
 */
public class Hull {
    /**
     * Index vrcholu vramci vstupneho pola
     */
    public final int i;
    
    /**
     * Referencie na nasledujuci/predosly vrchol
     */
    public Hull next, prev;

    /**
     * Inicializuje uzol obalu.
     * @param i 
     */
    public Hull(int i){
        this.i = i;
    }
    
    /**
     * Inicializuje vrchol a nastavi referencie na susedne prvky.
     * @param i
     * @param left
     * @param right 
     */
    public Hull(int i, Hull left, Hull right) {
        this.i = i;
        this.next = left;
        this.prev = right;
    }
}