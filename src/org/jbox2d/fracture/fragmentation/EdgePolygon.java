package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.util.MyList;

/**
 * Hrana obecneho polygonu.
 *
 * @author Marek Benovic
 */
class EdgePolygon extends AEdge {
    public EdgePolygon(Vec2 v1, Vec2 v2) {
        super(v1, v2);
    }
    
    /**
     * List prienikovych bodov, ktore sa nachadzaju na danej hrane.
     */
    public MyList<Vec2Intersect> list = new MyList<>();
}
