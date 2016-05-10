package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;

/**
 * Objekt sluziaci na rychle hladanie prienikovych bodov voronoi diagramu a
 * polygonu. Reprezentuje zaciatocny/koncovy bod usecky - udalost algoritmu zametania.
 *
 * @author Marek Benovic
 */
class EVec2 implements Comparable<EVec2> {
    /**
     * Hrana
     */
    public AEdge e;
    
    /**
     * Bod hrany
     */
    public Vec2 p;
    
    /**
     * True, pokial je dany bod zaciatocny, false ak je koncovy.
     */
    public boolean start;
    
    /**
     * Inicializuje bod
     * @param p
     */
    public EVec2(Vec2 p) {
        this.p = p;
    }

    @Override
    public int compareTo(EVec2 o) {
        Vec2 l = o.p;
        return p.y > l.y ? 1 : p.y == l.y ? (o.start == start ? 0 : (!start ? 1 : -1)) : -1;
    }
}
