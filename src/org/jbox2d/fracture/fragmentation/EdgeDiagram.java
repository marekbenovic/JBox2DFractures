package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.Fragment;

/**
 * Hrana Voronoi diagramu.
 *
 * @author Marek Benovic
 */
class EdgeDiagram extends AEdge {
    public EdgeDiagram(Vec2 v1, Vec2 v2) {
        super(v1, v2);
    }
    
    /**
     * Fragmenty, ktore ohranicuje dana hrana.
     */
    public Fragment d1, d2;
}
