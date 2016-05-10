package org.jbox2d.fracture.voronoi;

/**
 * Hrana delaunay triangulacie.
 *
 * @author Marek Benovic
 */
class Edge {
    /**
     * Referencie na hraniciace trojuholniky.
     */
    Triangle t1, t2;
    
    /**
     * Alokuje instanciu.
     */
    Edge() {
    }
    
    /**
     * Vymaze obsah instancie.
     */
    final void init() {
        t1 = null;
        t2 = null;
    }
    
    /**
     * Prida trojuholnik do hrany
     * @param triangle 
     */
    final void add(final Triangle t) {
        if (t1 == null) {
            t1 = t;
        } else {
            t2 = t;
        }
    }
    
    /**
     * Zmaze trojuholnik z hrany
     * @param t 
     */
    final void remove(final Triangle t) {
        if (t1 == t) {
            t1 = null;
        } else {
            t2 = null;
        }
    }
    
    /**
     * @param t
     * @return Vrati susedny trojuholnik, ktory nieje v parametri
     */
    final Triangle get(final Triangle triangle) {
        return t1 == triangle ? t2 : t1;
    }
    
    /**
     * @return Vrati trojuholnik (pocita sa, ze hrana ma minimalne jeden)
     */
    final Triangle get() {
        return t1 == null ? t2 : t1;
    }
    
    /**
     * @param i1 Index 1. vrcholu
     * @param i2 Index 2. vrcholu
     * @return Vrati index hrany
     */
    final static int index(final int i1, final int i2) {
        if (i1 < i2) {
            return i1 << 8 | i2;
        } else {
            return i2 << 8 | i1;
        }
    }
}