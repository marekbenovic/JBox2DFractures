package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.Polygon;

/**
 * Vrchol rovinneho grafu. Sluzi na zjednocovanie fragmentov polygonu.
 *
 * @author Marek Benovic
 */
class GraphVertex {
    /**
     * Vrchol v grafe
     */
    public Vec2 value;
    
    /**
     * Pocet polygonov, ktorych sucastou je dany vrchol
     */
    public int polygonCount;
    
    /**
     * Mnohosteny, ktorych je dana hrana sucastou.
     */
    public Polygon first, second;
    
    /**
     * Susedny vrchol cesty ohranicenia.
     */
    public GraphVertex next, prev;
    
    /**
     * Pomocna premenna sluziaca na vypocet.
     */
    boolean visited = false;
    
    /**
     * Inicializuje vrchol
     * @param value
     */
    public GraphVertex(Vec2 value) {
        this.value = value;
        this.polygonCount = 1;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec2) {
            Vec2 o2 = (Vec2) o;
            return value == o2;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}