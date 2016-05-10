package org.jbox2d.fracture.hertelmehlhorn;

import org.jbox2d.fracture.util.HashTable;
import org.jbox2d.fracture.util.Node;

/**
 * Hashovacia tabulka hran pre hertel-mehlhornov algoritmus.
 *
 * @author Marek Benovic
 */
class EdgeTable extends HashTable<Diagonal> {
    public Diagonal get(int i1, int i2) {
        for (Node<Diagonal> chain = super.hashtable[Diagonal.hashCode(i1, i2) & super.n]; chain != null; chain = chain.next) {
            Diagonal e = chain.value;
            if ((e.n11.index == i1 && e.n12.index == i2) || (e.n11.index == i2 && e.n12.index == i1)) {
                return e;
            }
        }
        return null;
    }
    public void remove(int i1, int i2) {
        Diagonal e = get(i1, i2);
        super.remove(e);
    }
    public void remove(Diagonal e) {
        remove(e.n11.index, e.n12.index);
    }
}