package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import static org.jbox2d.common.Vec2.cross;
import static org.jbox2d.fracture.fragmentation.Arithmetic.distanceSq;

/**
 * Hrana polygonu voronoi diagramu - sluzi na spracovanie prienikov polygonu
 * a voronoi rozdelenia.
 *
 * @author Marek Benovic
 */
abstract class AEdge {
    /**
     * Vrchol hrany.
     */
    Vec2 p1, p2;
    
    /**
     * Inicializuje vrcholy hrany
     * @param p1
     * @param p2 
     */
    protected AEdge(Vec2 p1, Vec2 p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
    
    /**
     * @param a
     * @param b
     * @return Vektorovy sucin
     */
    public static double dCross(Vec2 a, Vec2 b) {
        double ax = a.x;
        double ay = a.y;
        double bx = b.x;
        double by = b.y;
        return ax * by - bx * ay;
    }
    
    /**
     * @param a
     * @param b
     * @return Vrati prienik 2 hran. Pokial neexistuje, vrati null.
     */
    public static Vec2Intersect intersect(AEdge a, AEdge b) {
        Vec2 U = a.p2.sub(a.p1);
        Vec2 V = b.p2.sub(b.p1);
        Vec2 A = a.p1.clone();
        Vec2 C = b.p1.clone();
        double uv = dCross(U, V); //treba double precision (aby sa eliminovali aj ojedinele neziaduce pripady)
        if (uv == 0) {
            return null; //su to rovnobezky (ak su v jednej polohe, system to neosetruje)
        }
        double k = (dCross(C, V) - dCross(A, V)) / uv;
        double o = (dCross(C, U) - dCross(A, U)) / uv;
        if (o > 0 && o < 1 && k > 0 && k < 1) {
            double ux = U.x * k + A.x;
            double uy = U.y * k + A.y;
            A.set((float)ux, (float)uy);
            return new Vec2Intersect(A, k);
        } else {
            return null;
        }
    }
    
    /**
     * @param b1
     * @param b2
     * @return Vrati true, ak sa hrany pretinaju.
     */
    public boolean intersectAre(Vec2 b1, Vec2 b2) {
        Vec2 U = p2.sub(p1);
        Vec2 V = b2.sub(b1);
        Vec2 A = p1.clone();
        Vec2 C = b1.clone();
        float uv = cross(U, V);
        if (uv == 0) {
            return false; //su to rovnobezky (ak su v jednej polohe, system to neosetruje)
        }
        float k = (cross(C, V) - cross(A, V)) / uv;
        float o = (cross(C, U) - cross(A, U)) / uv;
        return o > 0 && o < 1 && k > 0 && k < 1;
    }
    
    /**
     * @param point
     * @return Vrati najvlizsi bod na priamke voci bodu z parametra.
     */
    public Vec2 kolmicovyBod(Vec2 point) {
        Vec2 U = p2.sub(p1);
        Vec2 V = new Vec2(p1.y - p2.y, p2.x - p1.x);
        float uv = cross(U, V);
        if (uv == 0) {
            return null; //su to rovnobezky (ak su v jednej polohe, system to neosetruje)
        }
        float k = (cross(point, V) - cross(p1, V)) / uv;
        if (k >= 0 && k <= 1) {
            U.mulLocal(k);
            return p1.add(U);
        } else {
            double dist1 = distanceSq(p1, point);
            double dist2 = distanceSq(p2, point);
            return dist1 < dist2 ? p1 : p2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AEdge) {
            AEdge d = (AEdge) o;
            return (d.p1 == p1 && d.p2 == p2) || (d.p1 == p2 && d.p2 == p1);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return p1.hashCode() ^ p2.hashCode();
    }
    
    @Override
    public String toString() {
        return "["+p1+"]-["+p2+"]";
    }
}
