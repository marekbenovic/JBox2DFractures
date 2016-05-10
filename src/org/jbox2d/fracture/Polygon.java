package org.jbox2d.fracture;

import org.jbox2d.fracture.fragmentation.Arithmetic;
import org.jbox2d.fracture.hertelmehlhorn.SingletonHM;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.jbox2d.common.Vec2;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Settings;
import org.jbox2d.fracture.poly2Tri.Triangulation;

/**
 * Polygon - je reprezentovany postupnostou vrcholov
 *
 * @author Marek Benovic
 */
public class Polygon implements Iterable<Vec2>, Cloneable {
    private static final float AABBConst = 1;
    private static final SingletonHM HM = new SingletonHM();
    
    /**
     * Pole vrcholov.
     */
    protected Vec2[] array;
    
    /**
     * Pocet vrcholov.
     */
    protected int count;
    
    /**
     * Vytvori prazdny polygon bez vrcholov. Polygon moze byt konvexny,
     * konkavny aj nonsimple.
     */
    public Polygon() {
        array = new Vec2[8];
        count = 0;
    }
    
    /**
     * Vytvori polygon z danych vrcholov. Vlozi dane body do polygonu. Pole
     * z parametra sa preda referenciou (nedochadza ku klonovaniu).
     * @param va Vstupne vrcholy.
     */
    public Polygon(Vec2[] va) {
        array = va;
        count = va.length;
    }
    
    /**
     * Vytvori polygon z danych vrcholov. Vlozi dane body do polygonu. Pole
     * z parametra sa preda referenciou (nedochadza ku klonovaniu).
     * @param va Vstupne vrcholy.
     * @param n Pocet aktivnych vrcholov
     */
    public Polygon(Vec2[] va, int n) {
        array = va;
        count = n;
    }
    
    /**
     * Vlozi do Polygonu prvky z kolekcie.
     * @param c Kolekcia s vrcholmi.
     */
    public void add(Collection<? extends Vec2> c) {
        for (Vec2 v : c) {
            add(v);
        }
    }

    /**
     * Prida vrchol do polygonu
     * @param v Pridavany vrchol
     */
    public void add(Vec2 v) {
        if (array.length == count) {
            Vec2[] newArray = new Vec2[count * 2];
            System.arraycopy(array, 0, newArray, 0, count);
            array = newArray;
        }
        array[count++] = v;
    }
    
    /**
     * @param index
     * @return Vrati prvok na danom mieste
     */
    public Vec2 get(int index) {
        return array[index];
    }
    
    /**
     * @return Vrati pocet prvkov
     */
    public int size() {
        return count;
    }

    /**
     * @param index Index bodu
     * @return Vrati vrchol podla poradia s osetrenim pretecenia.
     */
    public Vec2 cycleGet(int index) {
        return get(index % count);
    }
    
    /**
     * @return Vrati v poli vrcholy polygonu - vrati referenciu na interne pole,
     *         preto pri iterovani treba brat pocet cez funkciu size a nie
     *         cez array.length.
     */
    public Vec2[] getArray() {
        return array;
    }
    
    /**
     * Existuje efektivnejsia implementacia v pripade, ze bodov je viacej.
     * http://alienryderflex.com/polygon/
     * Este upravena by bola vziat vsetky hrany
     * 
     * @param p
     * @return Vrati true.
     */
    public boolean inside(Vec2 p) {
        int i, j;
        boolean c = false;
        Vec2 v = new Vec2();
        for (i = 0, j = count - 1; i < count; j = i++) {
            Vec2 a = get(i);
            Vec2 b = get(j);
            v.set(b);
            v.subLocal(a);
            if (((a.y >= p.y) != (b.y >= p.y)) && (p.x <= v.x * (p.y - a.y) / v.y + a.x)) {
                c = !c;
            }
        }
        return c;
    }
    
    /**
     * @return Vrati hmotnost telesa.
     */
    public double mass() {
        double m = 0;
        for (int i = 0, j = 1; i != count; i = j, j++) {
            Vec2 b1 = get(i);
            Vec2 b2 = get(j == count ? 0 : j);
            m += Vec2.cross(b1, b2);
        }
        m = Math.abs(m / 2);
        return m;
    }
    
    /**
     * @return Vrati tazisko polygonu.
     */
    public Vec2 centroid() {
        Vec2 C = new Vec2(); //centroid
        double m = 0;
        Vec2 g = new Vec2(); //pomocne vektor pre medzivypocet
        for (int i = 0, j = 1; i != count; i = j, j++) {
            Vec2 b1 = get(i);
            Vec2 b2 = get(j == count ? 0 : j);
            float s = Vec2.cross(b1, b2);
            m += s;
            g.set(b1);
            g.addLocal(b2);
            g.mulLocal(s);
            C.addLocal(g);
        }
        C.mulLocal((float) (1 / (3 * m)));
        return C;
    }
    
    /**
     * @return Vrati najvacsiu vzdialenost 2 bodov.
     */
    private double radius() {
        double ln = Float.MIN_VALUE;
        for (int i = 0; i < count; ++i) {
            Vec2 v1 = get(i);
            Vec2 v2 = cycleGet(i + 1);
            ln = Math.max(Arithmetic.distanceSq(v1, v2), ln);
        }
        return Math.sqrt(ln);
    }
    
    /**
     * @return Ak je polygon priliz maly, alebo tenky (nieje dobre ho zobrazovat), vrati false.
     */
    public boolean isCorrect() {
        double r = radius();
        double mass = mass();
        return (r > Material.MINFRAGMENTSIZE && mass > Material.MINFRAGMENTSIZE && mass / r > Material.MINFRAGMENTSIZE);
    }
    
    /**
     * @return Vrati AABB pre Polygon sluziaci na rozsah generovanych ohnisk pre
     *         fraktury. Preto je to umelo nafunknute o konstantu 1.
     */
    public AABB getAABB() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        if (count == 0) {
            return null;
        } else {
            for (int i = 0; i < count; ++i) {
                Vec2 v = get(i);
                minX = Math.min(v.x, minX);
                minY = Math.min(v.y, minY);
                maxX = Math.max(v.x, maxX);
                maxY = Math.max(v.y, maxY);
            }
        }
        return new AABB(new Vec2(minX - AABBConst, minY - AABBConst), new Vec2(maxX + AABBConst, maxY + AABBConst));
    }

    /**
     * @return Vezme dany polygon a urobi konvexnu dekompoziciu - rozpad na konvexne polygony
     *         s referencnou zavislostou (spolocne vrcholy polygonov su rovnake instancie).
     */
    public Polygon[] convexDecomposition() {
        if (isSystemPolygon()) { //optimalizacia - je zbytocne spustat algoritmus, ked to nieje zapotreby
            return new Polygon[] { this };
        }
       
        //tu pustim triangulacu z poly2tri
        Vec2[] reverseArray = new Vec2[count];
        for (int i = 0; i < count; ++i) {
            reverseArray[i] = get(count - i - 1);
        }
        
        ArrayList<ArrayList<Integer>> triangles = Triangulation.triangulate(reverseArray, count);

        int c = triangles.size();
        
        int[][] list = new int[c][3];
        for (int i = 0; i < c; i++) {
            ArrayList<Integer> t = triangles.get(i);
            int i1 = t.get(0);
            int i2 = t.get(1);
            int i3 = t.get(2);
            list[i][0] = i1;
            list[i][1] = i2;
            list[i][2] = i3;
        }

        HM.calculate(list, reverseArray, Settings.maxPolygonVertices);
        return HM.dekomposition;
    }
    
    /**
     * Otoci poradie prvkov v poli.
     */
    public void flip() {
        Vec2 temp;
        int size = size();
        int n = size() >> 1;
        for (int i = 0; i < n; i++) {
            temp = array[i];
            int j = size - 1 - i;
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * @return Vrati true, pokial je polygon konvexny a pocet vrcholov je maxPolygonVertices
     */
    private boolean isSystemPolygon() {
        return isConvex() && count <= Settings.maxPolygonVertices;
    }
    
    /**
     * @return Vrati true, pokial je polygon konvexny.
     */
    private boolean isConvex() {
        for (int i = 0; i < count; i++) {
            Vec2 a = get(i);
            Vec2 b = cycleGet(i + 1);
            Vec2 c = cycleGet(i + 2);
            if (Arithmetic.siteDef(a, b, c) == 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return Vrati true, pokial je postupnost vrcholov v smere hodinovych ruciciek
     */
    public boolean isClockwise() {
        double signedArea = 0;
        for (int i = 0; i < size(); ++i) {
            Vec2 v1 = get(i);
            Vec2 v2 = cycleGet(i + 1);
            double v1x = v1.x;
            double v1y = v1.y;
            double v2x = v2.x;
            double v2y = v2.y;
            signedArea += v1x * v2y - v2x * v1y;
        }
        return signedArea < 0;
    }
    
    
    /**
     * @return Vrati novy polygon. Pole je realokovane, ale referencie na
     *         body (Vec2) su povodne.
     */
    @Override
    public Polygon clone() {
        Vec2[] newArray = new Vec2[count];
        int newCount = count;
        System.arraycopy(array, 0, newArray, 0, count);
        return new Polygon(newArray, newCount);
    }
    
    /**
     * @return Vrati iterator na vrcholy polygonu
     */
    @Override
    public Iterator<Vec2> iterator() {
        return new MyIterator();
    }
    
    private class MyIterator implements Iterator<Vec2> {
        private int index;
        
        public MyIterator() {
            index = 0;
        }
        public MyIterator(int index) {
            this.index = index;
        }
        @Override
        public boolean hasNext() {
            return index < count;
        }
        @Override
        public Vec2 next() {
            return get(index++);
        }
    }
}
