package org.jbox2d.fracture;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.fracture.util.MyList;

/**
 * Sluzi na aplikovanie konkavnych polygonov do jbox2d enginu. Fixtury telesa,
 * ktore su produktom konvexnej dekompozicie, maju v sebe ulozenu referenciu na
 * PolygonFixture instanciu, ktory reprezentuje povodne konkavne teleso.
 * PolygonFixture zaroven obsahuje List referencii na jeho jednotlive konvexne
 * fixtures.
 * 
 * @author Marek Benovic
 */
public class PolygonFixture extends Polygon {
    /**
     * Inicializuje inicializujuci prazdny polygon.
     */
    public PolygonFixture() {
        super();
    }
    
    /**
     * Inicializuje polygon s mnozinou vrcholov z pamatera (referencne zavisle).
     * @param v 
     */
    public PolygonFixture(Vec2[] v) {
        super(v);
    }
    
    /**
     * Kopirovaci konstruktor. Inicializuje jednoduchym kopirovanim referencie.
     * @param pg 
     */
    PolygonFixture(Polygon pg) {
        array = pg.getArray();
        count = pg.size();
    }
    
    /**
     * Mnozina fixture, z ktorych dany polygon pozostava (reprezentuju konvexnu
     * dekompoziciu).
     */
    public MyList<Fixture> fixtureList = new MyList<>();
}
