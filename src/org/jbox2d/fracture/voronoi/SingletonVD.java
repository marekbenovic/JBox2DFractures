package org.jbox2d.fracture.voronoi;

import java.util.Arrays;
import java.util.Random;
import org.jbox2d.common.Vec2;

/**
 * Voronoi diagram - perfektna cista dokonala implementacia ktora zozerie akykolvek
 * vstup, vrati chybu, ak su tam duplicity, alebo null, alebo notFinite cisla
 * max 256 ohnisk, super optimalizacia, minimalizacia alokacie pamate, moznost
 * multithreadingu - kazda instancia Voronoi sa bude volat v samostatnom vlakne.
 * GPU akceleracia alebo parallelizmus tam moc nehra. vsetko bude na CPU.
 * Minimalisticke rozhranie, minimalny pocet tried, co najjednoduchsie.
 * Hlavne pamatove naroky ma tabulka Edges, ktora je kvadraticka -> ta spolu
 * s hranami robi cca 1MB. Vdaka tomu vsak kniznica je relativne vykonna:
 * 
 * Vstup: body musia mat Float.finite hodnoty, nesmu byt duplicitne a pre voronoi
 * diagram sa musia nachadzat v ohraniceni.
 * 
 * 23.0 kf (0.0000434s) na generovanie 100 bodov pre delaunay triangulaciu
 * 15.6 kF (0.0000641s) na generovanie 100 bodov voronoi diagramu
 *
 * @author Marek Benovic
 */
public class SingletonVD {
    /**
     * Body voronoi diagramu. Rozmedzie platnych bodov: (0, pCount - 1).
     * Nenastavovat hodnotam null value.
     */
    public final Vec2[] points = new Vec2[0x404]; //max pocet bodov polygonu je pocet trojuholnikov (3x) + pocet boundaries (1x) + rohy (4)
    
    /**
     * Pocet bodov voronoi diagramu.
     */
    public int pCount = 0;
    
    /**
     * Vystupne indexy bodov vo voronoi diagrame. Voronoi obsahuje pole polygonov (int[]).
     * Kazdy z nich ma podla prislusneho indexu definovanu dlzku v vCount. Pocet
     * polygonov je rovnaky ako pocet ohnisk parametra vo volani calculateVoronoi.
     * Nenastavovat ziadnemu prvku hodnotu null.
     * Ked bude ze {2,1,4}, {1,2,0}, {1,2,5,6} - su tam 3 polygony a body su indexy z pola points - pre kazdy polygon focee su ohniska.
     */
    public final int[][] voronoi = new int[0x100][0x104];
    
    /**
     * Pocty vrcholov polygonov voronoi diagramu.
     */
    public final int[] vCount = new int[0x100];
    
    /**
     * Delaunay triangulacia - triangles su trojuholniky triangulacie. Rozmedzie
     * platnych trojuholnikov: (0, triangC - 1). Nenastavovat hodnotam null value.
     */
    public final Triangle[] triangles = new Triangle[0x300];
    
    /**
     * Vstupne pole ohnisk.
     */
    public Vec2[] ar;
    
    /**
     * Pocet trojuholnikov triangulacie.
     */
    public int triangC = 0;
    
    /**
     * Obojsmerny spojovy zoznam prvkov konvexneho obalu.
     */
    public Hull hull;
    
    private static final double RND = new Random().nextDouble() + 0.5;
    private static final double SIN = Math.sin(RND);
    private static final double COS = Math.cos(RND);
    
    private final Edge[] edges = new Edge[0x10000]; //2D pole hran
    private final int[] boundaries = new int[0x200];
    private int boundariesCount = 0;
    private float maxX, minX, minY, maxY;
    private final double[] comparer = new double[0x302]; //6 kb
    private final boolean[] validCorners = new boolean[4]; //true znamena, ze je platny roh, false, ze je neplatny... neplatny roh je vtedy, ked boundaries vypocita bod s rovnakymi suradnicami (nepravdepodobne, ale mozne)
    private int[] polygon; //pomocna premenna

    /**
     * Inicializuje Factory. Samotny geometricky vypocet je potom osetreny
     * o pomalu alokaciu pamate.
     */
    public SingletonVD() {
        for (int i = 0; i < triangles.length; ++i) {
            triangles[i] = new Triangle(i); //to robi asi 40kB
        }
        for (int i = 0; i < points.length; ++i) {
            points[i] = new Vec2(); //to robi asi 40kB
        }
        for (int i = 0; i < 0x100; ++i) {
            for (int j = i + 1; j < 0x100; ++j) {
                edges[(i << 8) | j] = new Edge();
            }
        }
    }
    
    /**
     * Pocita voronoi diagram na zaklade vstupnych parametrov a vklada ich do
     * vyslednych prvkov.
     * @param focee
     * @param a
     * @param b 
     */
    public void calculateVoronoi(Vec2[] focee, final Vec2 a, final Vec2 b) {
        pCount = 0;

        maxX = Math.max(a.x, b.x);
        maxY = Math.max(a.y, b.y);
        minX = Math.min(a.x, b.x);
        minY = Math.min(a.y, b.y);

        calculateDelaunay(focee);
        int count = focee.length;
        
        if (count == 0) {
            return;
        }
        
        Arrays.fill(vCount, 0, count, 0);
        boundariesCount = 0;
        
        points[0].set(maxX, maxY);
        points[1].set(maxX, minY);
        points[2].set(minX, minY);
        points[3].set(minX, maxY);
        pCount = 4;
        
        if (count == 1) {
            int[] p = voronoi[0];
            p[0] = 0;
            p[1] = 1;
            p[2] = 2;
            p[3] = 3;
            vCount[0] = 4;
        } else {
            Arrays.fill(validCorners, true);
            
            for (int i = 0; i != triangC; ++i) {
                Triangle triangle = triangles[i];
                double x = triangle.dX;
                double y = triangle.dY;

                if (x <= maxX && x >= minX && y <= maxY && y >= minY) { //tu osetrim, ci je bod voronoi diagramu v obdlzniku
                    int ti = triangle.i;
                    int tj = triangle.j;
                    int tk = triangle.k;
                    
                    voronoi[ti][vCount[ti]++] = voronoi[tj][vCount[tj]++] = voronoi[tk][vCount[tk]++] = pCount;
                    points[pCount++].set((float) triangle.dX, (float) triangle.dY);
                }
            }

            Hull left = hull;
            do {
                Hull right = left.prev;
                addBoundary(left.i, right.i, null); //rekurzivna funkcia
                left = right;
            } while (left != hull);

            for (int i = 0; i != boundariesCount; ++i) {
                int li = boundaries[(i == 0 ? boundariesCount : i) - 1];
                int ri = boundaries[i];
                Vec2 l = ar[li];
                Vec2 r = ar[ri];

                float sy = l.x > r.x ? minY : maxY;
                float y = sy; //ak ==, je to za jedno, x bude +-Infinity a vykona sa podmienka
                float x = (float) x(l, r, y);

                if (x >= maxX || x <= minX) { //vypocitane hodnoty niesu korektne, treba prehodit suradnice, ak ==, je to v poriadku, netreba upravovat
                    x = l.y > r.y ? maxX : minX;
                    y = (float) y(l, r, x);
                    
                    if (y == sy) { //nastane velmi nepravdepodobne, je to mozne
                        for (int j = 0; j != 4; ++j) {
                            Vec2 c = points[j];
                            if (c.x == x && c.y == y) {
                                validCorners[j] = false;
                                break;
                            }
                        }

                    }
                }

                voronoi[li][vCount[li]++] = voronoi[ri][vCount[ri]++] = pCount;
                points[pCount++].set(x, y);
                
            }

            for (int i = 0; i != 4; ++i) {
                if (validCorners[i]) {
                    Vec2 corner = points[i]; //suradnice rohu

                    double x = corner.x;
                    double y = corner.y;

                    int min = boundaries[0];
                    double distMin = Double.MAX_VALUE;
                    for (int j = 0; j != boundariesCount; ++j) {
                        int point = boundaries[j];
                        double dist = distSq(x, y, ar[point]);
                        if (dist < distMin) {
                            distMin = dist;
                            min = point;
                        }
                    }

                    voronoi[min][vCount[min]++] = i;
                }
            }

            for (int i = 0; i < focee.length; ++i) {
                sort(i);
            }
        }
    }
    
    /**
     * Zotriedi vrcholy v polygone podla uhlu
     * @param indexPolygon 
     */
    private void sort(int indexPolygon) {
        int size = vCount[indexPolygon];
        if (size != 0) {
            polygon = voronoi[indexPolygon];
            Vec2 focus = ar[indexPolygon];
            for (int i = 0; i != size; ++i) {
                comparer[i] = angle(points[polygon[i]], focus);
            }
            quicksortPoly(0, size - 1);
        }
    }

    private void quicksortPoly(final int low, final int high) {
        int i = low, j = high;
        double pivot = comparer[low + ((high - low) >> 1)]; // Get the pivot element from the middle of the list
        while (i <= j) {
            while (comparer[i] < pivot) {
                i++;
            }
            while (comparer[j] > pivot) {
                j--;
            }
            if (i <= j) {
                double temp = comparer[i];
                comparer[i] = comparer[j];
                comparer[j] = temp;
                int v = polygon[i];
                polygon[i] = polygon[j];
                polygon[j] = v;
            
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksortPoly(low, j);
        }
        if (i < high) {
            quicksortPoly(i, high);
        }
    }

    /**
     * @param low
     * @param high 
     */
    private void quicksort(final int low, final int high) {
        int i = low, j = high;
        double pivot = comparer[low + ((high - low) >> 1)]; // Get the pivot element from the middle of the list
        while (i <= j) {
            while (comparer[i] < pivot) {
                i++;
            }
            while (comparer[j] > pivot) {
                j--;
            }
            if (i <= j) {
                double temp = comparer[i];
                comparer[i] = comparer[j];
                comparer[j] = temp;
                Vec2 v = ar[i];
                ar[i] = ar[j];
                ar[j] = v;
            
                i++;
                j--;
            }
        }
        if (low < j) {
            quicksort(low, j);
        }
        if (i < high) {
            quicksort(i, high);
        }
    }

    /**
     * Vypocet delaunay triangulacie
     * @param ar Pole ohnisk
     */
    public void calculateDelaunay(Vec2[] ar) {
        this.ar = ar;
        triangC = 0;
        hull = null;

        int size = Math.min(this.ar.length, 0x100);
        
        if (size == 0) {
            return;
        }

        for (int i = 0; i != size; ++i) {
            Vec2 v = this.ar[i];
            comparer[i] = SIN * v.x + COS * v.y;
        }
        quicksort(0, size - 1); //zotriedi body podla transformacie (transformacia osetruje specialne nepravdepodobne pripady)
        
        /* //pre bakalarku ukazka fungovania osetrenia sialenych vstupov (mozno to tam ani pisat nebudes, lebo tazko zistit preco to robi)
        Arrays.sort(f, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Vec2 v1 = (Vec2) o1;
                Vec2 v2 = (Vec2) o2;
                return v1.x > v2.x ? 1 : v1.x == v2.x ? v1.y > v2.y ? 1 : -1 : -1;
            }
        });
        */
        
        hull = new Hull(0);
        hull.prev = hull.next = hull;
        for (int v = 1; v != size; ++v) {
            Hull r = hull;
            Hull l = hull;

            while (site(this.ar[v], this.ar[r.i], this.ar[r.prev.i]) == 1) {
                r = r.prev;
            }
            while (site(this.ar[v], this.ar[l.i], this.ar[l.next.i]) == -1) {
                l = l.next;
            }
            Edge left = edges[Edge.index(l.i, v)];
            left.init();

            //ten hull zachadza za roh, to je nejaka kktina
            for (Hull k = l; k != r; k = k.prev) {
                int lp = k.i;
                int rp = k.prev.i;
                Edge right = edges[Edge.index(rp, v)];
                right.init();
                addTriangle(left, right, edges[Edge.index(lp, rp)], lp, rp, v);
                left = right;
            }
            if (r == l && hull.prev != hull) { //duplicitna linia v uvodu pri prazdnej triangulacii - vsetky body su na jednej priamke
                Hull duplicitny = new Hull(hull.i);
                Hull novy = new Hull(v, duplicitny, hull);
                duplicitny.next = hull.next;
                duplicitny.prev = novy;
                hull.next.prev = duplicitny;
                hull.next = novy;
                hull = novy;
            } else {
                hull = r.next = l.prev = new Hull(v, l, r);
            }
        }
    }
    
    /**
     * @param a
     * @param b
     * @param t 
     */
    private void addBoundary(int a, int b, Triangle t) {
        Triangle opposite = edges[Edge.index(a, b)].get(t);
        Vec2 va = ar[a];
        Vec2 vb = ar[b];
        boolean bx = va.x > vb.x; //na rovnosti nezalezi - ak nastane rovnost, kontroluje sa len 1 rozmer (x) a ten sa vzdy trafi do stredu, takze obe podmienky ohranicujuce budu false
        boolean by = va.y > vb.y; //na rovnosti nezalezi - ak nastane rovnost, kontroluje sa len 1 rozmer (y)
        if (opposite != null) {
            float x = (float) opposite.dX;
            float y = (float) opposite.dY;
            if (bx && y <= minY || !bx && y >= maxY || by && x >= maxX || !by && x <= minX) { //vlozim rovnost - to mi zaruci, ze pri spracovani boundaries nemusim riesit duplicity
                int center = opposite.get(a, b);
                addBoundary(a, center, opposite);
                addBoundary(center, b, opposite);
                return;
            }
            //ak sa nachadzaju na hrane, tak je to v poriadku - voronoi osetri duplicity
        }
        boundaries[boundariesCount++] = b;
    }
    
    /**
     * @param leftEdge
     * @param rightEdge
     * @param centerEdge
     * @param l
     * @param r
     * @param v 
     */
    private void addTriangle(
        final Edge leftEdge,
        final Edge rightEdge,
        final Edge centerEdge,
        final int l,
        final int r,
        final int v
    ) {
        Triangle triangle = centerEdge.get();
        if (triangle == null || !triangle.inside(ar[v])) {
            //efektivne aplokuje triangle (pouziva stare referencie)
            Triangle newTriangle = triangles[triangC++];
            newTriangle.init(l, r, v, ar, triangle);
            leftEdge.add(newTriangle);
            rightEdge.add(newTriangle);
            centerEdge.add(newTriangle);
        } else {
            int c = triangle.get(l, r);
            Edge center1 = edges[Edge.index(l, c)];
            Edge center2 = edges[Edge.index(r, c)];
            center1.remove(triangle);
            center2.remove(triangle);
            centerEdge.init();
            
            //pretoci hranu (staru poziciu netreba nulovat), taktiez redukcia alokacie objektu Edges neovplivnuje vykon nijakym sposobom
            Edge newCenterEdge = edges[Edge.index(c, v)];
            newCenterEdge.init();

            //vymaze triangle - robi to efektivnym sposobom, ktory minimalizuje (uplne z dlhodobeho hladiska uplne eliminuje alokaciu pamate)
            //vymaze triangle na indexi i - vymeni ho s poslednym trianglom
            int i = triangle.index;
            Triangle deleted = triangles[i];
            Triangle t = triangles[i] = triangles[--triangC];
            triangles[triangC] = deleted;
            deleted.index = triangC;
            t.index = i;
            
            //rekurzivne volanie na novovzniknute trojuholniky
            addTriangle(leftEdge, newCenterEdge, center1, l, c, v); //hrana medzi novymi trojuholnikmi
            addTriangle(newCenterEdge, rightEdge, center2, c, r, v); //rekurzivne volanie
        }
    }
    
    /**
     * @param a 1. bod priamky
     * @param b 2. bod priamky
     * @param v Bod, u ktoreho sa rozhoduje, na ktorej strane priamky sa nachadza
     * @return -1 ak je v nalavo od priamky a -> b, 1 ak napravo, 0 ak na nej
     */
    private static int site(final Vec2 a, final Vec2 b, final Vec2 v) {
        double ax = a.x;
        double ay = a.y;
        double bx = b.x;
        double by = b.y;
        double vx = v.x;
        double vy = v.y;
        double g = (bx - ax) * (vy - by);
        double h = (vx - bx) * (by - ay);
        return g > h ? 1 : g == h ? 0 : -1;
    }
    
    /**
     * @param a
     * @param b
     * @return Vrati kvadraticku hodnotu uhlu zvierajucom usecka (a, b) v intervale od 0-4
     */
    private static double angle(Vec2 a, Vec2 b) {
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double x = vx * vx;
        double cos = x / (x + vy * vy); //neni to linearne vzhladom na uzol - kvoli optimalizacii sa odstranila odmocnina, ale to nevadi
        return vx > 0 ? vy > 0 ? 3 + cos : 1 - cos : vy > 0 ? 3 - cos : 1 + cos;
    }
    
    /**
     * @param a
     * @param b
     * @param y
     * @return Vrati x suradnicu bodu, ktory je rovnako vzdialeny od a, b s y-sradnicou.
     */
    private static double x(Vec2 a, Vec2 b, double y) {
        double cx = a.x - b.x;
        double cy = a.y - b.y;
        return ((a.x + b.x) * cx + (a.y + b.y) * cy - 2 * y * cy) / (2 * cx);
    }
    
    /**
     * @param a
     * @param b
     * @param x
     * @return Vrati y suradnicu bodu, ktory je rovnako vzdialeny od a, b s x-sradnicou.
     */
    private static double y(Vec2 a, Vec2 b, double x) {
        double cx = a.x - b.x;
        double cy = a.y - b.y;
        return ((a.x + b.x) * cx + (a.y + b.y) * cy - 2 * x * cx) / (2 * cy);
    }
    
    /**
     * @param a
     * @param b
     * @return Vrati vzdialenost ^ 2 bodov
     */
    private static double distSq(double x, double y, final Vec2 b) {
        x -= b.x;
        y -= b.y;
        return x * x + y * y;
    }
}
