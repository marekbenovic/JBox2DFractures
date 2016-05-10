package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.util.HashTable;
import org.jbox2d.fracture.Polygon;
import org.jbox2d.fracture.Fragment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jbox2d.fracture.util.MyList;
import org.jbox2d.fracture.voronoi.SingletonVD;

/**
 * Hlavny objekt, ktory robi prienik voronoi diagramu generovany ohniskami
 * a polygonu predanom v parametri.
 *
 * @author Marek Benovic
 */
public class Singleton {
    private static final SingletonVD factory = new SingletonVD();
    
    /**
     * Mnozina vyslednych fragmentov.
     */
    public Polygon[] fragments;
    
    private Vec2[] focee;
    private Polygon p;
    
    float constant[]; //storage for precalculated constants (same size as polyX)
    float multiple[]; //storage for precalculated multipliers (same size as polyX)
    
    
    /**
     * Vrati prienik voronoi diagramu a polygonu.
     * 
     * @param focee
     * @param p Kopia polygonu, moze byt modifikovana
     * @param contactPoint Bod dotyku
     * @param ic Funkcionalny interface, ktory definuje, ci fragment patri,
     *           alebo nepatri do mnoziny ulomkov
     */
    public void calculate(Polygon p, Vec2[] focee, Vec2 contactPoint, IContains ic) {
        this.focee = focee;
        this.p = p;
        //Geometry geom = new Geometry(foceeAll, p);
        List<Fragment> list = getVoronoi();
        
        List<EdgePolygon> polygonEdgesList = new ArrayList<>();
        HashTable<EdgeDiagram> diagramEdges = new HashTable<>();
        HashTable<EdgePolygon> polygonEdges = new HashTable<>();
        
        //vlozim hrany polygonu do hashovacej tabulky hran polygonu
        int count = p.size();
        for (int i = 1; i <= count; i++) {
            Vec2 p1 = p.get(i - 1);
            Vec2 p2 = p.get(i == count ? 0 : i);
            EdgePolygon e = new EdgePolygon(p1, p2);
            polygonEdges.add(e);
            polygonEdgesList.add(e);
        }
        
        //vlozim hrany diagramu do hashovacej tabulky hran diagramu
        for (Fragment pp : list) {
            count = pp.size();
            for (int i = 1; i <= count; i++) {
                Vec2 p1 = pp.get(i - 1);
                Vec2 p2 = pp.get(i == count ? 0 : i);
                EdgeDiagram e = new EdgeDiagram(p1, p2);
                EdgeDiagram alternative = diagramEdges.getObject(e);
                if (alternative == null) {
                    diagramEdges.add(e);
                    e.d1 = pp;
                } else {
                    alternative.d2 = pp;
                }
            }
        }
        
        AEdge[][] allEdges = new AEdge[][] {
            diagramEdges.toArray(new AEdge[0]),
            polygonEdges.toArray(new AEdge[0])
        };
        
        ArrayList<EVec2> vectorList = new ArrayList<>();

        for (AEdge[] array : allEdges) {
            for (AEdge e : array) {
                EVec2 v1 = new EVec2(e.p1);
                EVec2 v2 = new EVec2(e.p2);
                v1.e = e;
                v2.e = e;
                if (v1.p.y < v2.p.y) {
                    v1.start = true;
                } else {
                    v2.start = true;
                }
                vectorList.add(v1);
                vectorList.add(v2);
            }
        }
        
        EVec2[] vectors = vectorList.toArray(new EVec2[0]);
        
        Arrays.sort(vectors); //zotriedim body

        diagramEdges.clear();
        polygonEdges.clear();

        for (EVec2 e : vectors) {
            if (e.e instanceof EdgeDiagram) {
                if (e.start) {
                    EdgeDiagram ex = (EdgeDiagram) e.e;
                    diagramEdges.add(ex);
                    
                    EdgePolygon[] hranyPolygonu = polygonEdges.toArray(new EdgePolygon[0]);
                    for (EdgePolygon px : hranyPolygonu) {
                        process(px, ex);
                    }
                    
                } else {
                    diagramEdges.remove((EdgeDiagram) e.e);
                }
            } else { //je instanciou EdgePolygon
                if (e.start) {
                    EdgePolygon px = (EdgePolygon) e.e;
                    polygonEdges.add(px);
                    
                    EdgeDiagram[] hranyPolygonu = diagramEdges.toArray(new EdgeDiagram[0]);
                    
                    for (EdgeDiagram ex : hranyPolygonu) {
                        process(px, ex);
                    }
                    
                } else {
                    polygonEdges.remove((EdgePolygon) e.e);
                }
            }
        }
            
        for (Fragment pol : list) {
            pol.resort();
            for (int i = 0; i < pol.size(); i++) {
                Vec2 v = pol.get(i);
                if (v instanceof Vec2Intersect) {
                    Vec2Intersect vi = (Vec2Intersect) v;
                    if (vi.p1 == pol) {
                        vi.i1 = i;
                    } else {
                        vi.i2 = i;
                    }
                }
            }
        }
        
        Polygon polygonAll = new Polygon();
        
        Comparator<Vec2Intersect> c = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Vec2Intersect v1 = (Vec2Intersect) o1;
                Vec2Intersect v2 = (Vec2Intersect) o2;
                return Double.compare(v1.k, v2.k);
            }
        };
        
        for (EdgePolygon ex : polygonEdgesList) {
            polygonAll.add(ex.p1);
            Collections.sort(ex.list, c);
            polygonAll.add(ex.list);
        }
        
        for (int i = 0; i < polygonAll.size(); i++) {
            Vec2 v = polygonAll.get(i);
            if (v instanceof Vec2Intersect) {
                Vec2Intersect vi = (Vec2Intersect) v;
                vi.index = i;
            }
        }

        MyList<Fragment> allIntersections = new MyList<>();
        //ostatne algoritmy generovali diery - tento je najlepsi - najdem najblizsi bod na hrane polygonu a zistim kolizny fargment - od neho prehladavam do sirky a kontrolujem vzdialenost a viditelnost (jednoduche, ciste)
        
        precalc_values();
        
        for (Fragment ppp : list) {
            List<Fragment> intsc = getIntersections(ppp, polygonAll);
            if (intsc == null) { // cely polygon sa nachadza vnutri fragmentu
                fragments = new Polygon[] { p };
                return;
            }
            allIntersections.addAll(intsc);
        }
        
        //vytvoria sa hrany a nastane prehladavanie do sirky
        //vytvorim hashovaciu tabulku hran
        HashTable<EdgeDiagram> table = new HashTable<>();
        for (Fragment f : allIntersections) {
            for (int i = 0; i < f.size(); ++i) {
                Vec2 v1 = f.get(i);
                Vec2 v2 = f.cycleGet(i + 1);
                EdgeDiagram e = new EdgeDiagram(v1, v2);
                EdgeDiagram e2 = table.getObject(e);
                if (e2 != null) {
                    e = e2;
                    e.d2 = f;
                } else {
                    e.d1 = f;
                    table.add(e);
                }
            }
        }
        
        EdgeDiagram[] ee = table.toArray(new EdgeDiagram[0]);
        
        //rozdelim polygony na 2 mnoziny - na tie, ktore budu ulomky a tie, ktore budu spojene a drzat spolu
        double distance = Double.MAX_VALUE;
        Fragment startPolygon = null;
        Vec2 kolmicovyBod = null;
        MyList<EdgeDiagram> allEdgesPolygon = new MyList<>();

        for (EdgeDiagram ep : ee) {
            if (ep.d2 == null) {

                //toto sa nahradi vzorcom na vypocet vzdialenosti body od usecky
                Vec2 vv = ep.kolmicovyBod(contactPoint);
                double newDistance = Arithmetic.distanceSq(contactPoint, vv);
                if (newDistance <= distance) {
                    distance = newDistance;
                    kolmicovyBod = vv;
                    startPolygon = ep.d1;
                }
                allEdgesPolygon.add(ep);
            }
        }

        MyList<Fragment> ppx = new MyList<>();
        ppx.add(startPolygon);
        EdgeDiagram epx = new EdgeDiagram(null, null);
        HashTable<Fragment> vysledneFragmenty = new HashTable<>();
        startPolygon.visited = true;

        while (!ppx.isEmpty()) {
            Fragment px = ppx.get(0);
            vysledneFragmenty.add(px);
            
            for (int i = 0; i < px.size(); ++i) {
                Vec2 v1 = px.get(i);
                Vec2 v2 = px.cycleGet(i + 1);
                epx.p1 = v1;
                epx.p2 = v2;
                EdgeDiagram ep = table.getObject(epx);
                Fragment opposite = ep.d1 == px ? ep.d2 : ep.d1;
                
                if (opposite != null && !opposite.visited) {
                    Vec2 centroid = opposite.centroid();
                    opposite.visited = true;
                    if (ic.contains(centroid)) {
                        boolean intersection = false;
                        for (EdgeDiagram edge : allEdgesPolygon) {
                            //neberie do uvahy hrany polygonu
                            if (edge.d1 != startPolygon && edge.d2 != startPolygon && edge.intersectAre(centroid, kolmicovyBod)) {
                                intersection = true;
                                break;
                            }
                        }

                        //tu bude podmienka - ci ten polygon vezmem do uvahy, ak hej, priplnim ho do MyListu
                        if (!intersection) {
                            ppx.add(opposite);
                        }
                    }
                }

            }

            ppx.removeAt(0);
        }
        
        Fragment[] fragmentsArray = vysledneFragmenty.toArray(new Fragment[0]);
        MyList<Fragment> fragmentsBody = new MyList<>();
        for (Fragment fx : allIntersections) {
            if (!vysledneFragmenty.contains(fx)) {
                fragmentsBody.add(fx);
            }
        }

        MyList<Polygon> result = zjednotenie(fragmentsBody);
        
        result.add(fragmentsArray);
        fragments = new Polygon[result.size()];
        result.addToArray(fragments);
    }
    
    /**
     * @param p1 Fragment 1
     * @param p2 Fragment 2
     * @return Vrati list polygonov, ktore su prienikmi polygonov z parametra.
     */
    private List<Fragment> getIntersections(Fragment p1, Polygon p2) {
        Vec2Intersect firstV = null;
        boolean idemPoKonvexnom = false;
        List<Fragment> polygonList = new ArrayList<>();
        
        for (Vec2 v : p1) {
            if (v instanceof Vec2Intersect) {
                firstV = (Vec2Intersect) v;
                
                Vec2 p2Next = p2.cycleGet(firstV.index + 1);
                Vec2 p1Back = p1.cycleGet((firstV.p1 == p1 ? firstV.i1 : firstV.i2) + 1);
        
                if (Arithmetic.siteDef(p1Back, firstV, p2Next) >= 0) {
                    break;
                }
            }
        }
        
        if (firstV == null) {
            if (pointInPolygon(p1.get(0))) {
                polygonList.add(p1);
            } else if (p1.inside(p2.get(0))) {
                return null;
            }
            return polygonList;
        }
        
        Vec2 start = firstV;
        
        Vec2 iterator;
        Polygon iterationPolygon = p2;
        int index = firstV.index;
        
        int exI = 0;
        cyklus: for (;;) {
            Fragment prienik = new Fragment();
            do {
                exI++;
                if (exI >= 10000) {
                    throw new RuntimeException();
                }
                
                iterator = iterationPolygon.cycleGet(++index);
                if (iterator instanceof Vec2Intersect) {
                    Vec2Intersect intersect = (Vec2Intersect) iterator;
                    prienik.add(intersect.vec2);
                    intersect.visited = true;
                    idemPoKonvexnom = !idemPoKonvexnom;
                    iterationPolygon = idemPoKonvexnom ? p1 : p2;
                    index = idemPoKonvexnom ? intersect.p1 == p1 ? intersect.i1 : intersect.i2 : intersect.index;
                } else {
                    prienik.add(iterator);
                }
            } while (iterator != firstV);
            polygonList.add(prienik);
            
            iterationPolygon = p1;
            index = iterationPolygon == firstV.p1 ? firstV.i1 : firstV.i2;
            idemPoKonvexnom = true;
            for (;;) {
                iterator = iterationPolygon.cycleGet(++index);
                if (iterator == start) {
                    break cyklus;
                }
                if (iterator instanceof Vec2Intersect) {
                    firstV = (Vec2Intersect) iterator;
                    if (!firstV.visited) {
                        break;
                    }
                }
                
                exI++;
                if (exI >= 10000) {
                    throw new RuntimeException();
                }
                
            }
        }
        for (Vec2 v : p1) {
            if (v instanceof Vec2Intersect) {
                Vec2Intersect vi = (Vec2Intersect) v;
                vi.visited = false;
            }
        }
        return polygonList;
    }
    
    /**
     * @return Vygeneruje list fragmentov voronoi diagramu na zaklade vstupnych
     *         ohnisk z clenskej premennej focee.
     */
    private List<Fragment> getVoronoi() {
        Vec2 min = new Vec2(Float.MAX_VALUE, Float.MAX_VALUE);
        Vec2 max = new Vec2(Float.MIN_VALUE, Float.MIN_VALUE);
        
        for (Vec2 v : p) {
            min = Vec2.min(min, v);
            max = Vec2.max(max, v);
        }
        for (Vec2 v : focee) {
            min = Vec2.min(min, v);
            max = Vec2.max(max, v);
        }
        
        Vec2 deficit = new Vec2(1, 1);
        min.subLocal(deficit);
        max.addLocal(deficit);
        
        factory.calculateVoronoi(focee, min, max);
        
        List<Fragment> fragmentList = new ArrayList<>();
        
        Vec2[] pp = new Vec2[factory.pCount];
        for (int i = 0; i < factory.pCount; ++i) {
            pp[i] = new Vec2(factory.points[i]);
        }
        
        for (int i = 0; i < focee.length; i++) {
            Fragment f = new Fragment();
            int n = factory.vCount[i];
            int[] ppx = factory.voronoi[i];
            for (int j = 0; j < n; ++j) {
                f.add(pp[ppx[j]]);
            }
            f.focus = focee[i];
            fragmentList.add(f);
        }
        
        return fragmentList;
    }
    
    /**
     * Vezme polygony a vrati ich zjednotenie. Plygony su navzajom disjunknte
     * avsak dotykaju sa bodmi hranami, ktore maju referencnu zavislost.
     * 
     * @param polygony
     * @return Vrati List zjednotenych polygonov.
     */
    private static MyList<Polygon> zjednotenie(MyList<Fragment> polygony) {
        HashTable<GraphVertex> graf = new HashTable<>();
        for (Polygon p : polygony) {
            for (int i = 1; i <= p.size(); ++i) {
                Vec2 v = p.cycleGet(i);
                GraphVertex vertex = graf.getObject(v);
                if (vertex == null) {
                    vertex = new GraphVertex(v);
                    graf.add(vertex);
                    vertex.first = p;
                } else {
                    vertex.polygonCount++;
                    vertex.second = p;
                }
            }
        }
        
        for (Polygon p : polygony) {
            for (int i = 0; i < p.size(); ++i) {
                GraphVertex v1 = graf.getObject(p.get(i));
                GraphVertex v2 = graf.getObject(p.cycleGet(i + 1));
                if (v1.polygonCount == 1 || v2.polygonCount == 1 || (v1.polygonCount <= 2 && v2.polygonCount <=2 && !((v1.first == v2.first && v1.second == v2.second) || (v1.first == v2.second && v1.second == v2.first)))) {
                    v1.next = v2;
                    v2.prev = v1;
                }
            }
        }
        
        MyList<Polygon> vysledok = new MyList<>();
        
        GraphVertex[] arr = graf.toArray(new GraphVertex[0]);
        for (GraphVertex v : arr) {
            if (v.next != null && !v.visited) {
                Polygon p = new Polygon();
                for (GraphVertex iterator = v; !iterator.visited; iterator = iterator.next) {
                    if (Arithmetic.siteDef(iterator.next.value, iterator.value, iterator.prev.value) != 0) {
                        p.add(iterator.value);
                    }
                    iterator.visited = true;
                }
                vysledok.add(p);
            }
        }
        
        return vysledok;
    }

    /**
     * Najde prienik 2 hran a spracuje vysledky.
     * @param a Hrana polygonu
     * @param b Hrana vo voronoi diagrame
     */
    private static void process(EdgePolygon a, EdgeDiagram b) {
        Vec2Intersect p = AEdge.intersect(a, b);
        if (p != null) {
            p.p1 = b.d1;
            p.p2 = b.d2;
            b.d1.add(p);
            b.d2.add(p);
            a.list.add(p);
        }
    }
    
    /**
     * Predpocita hodnoty pre zistovanie prieniku polygonu s bodmi
     */
    private void precalc_values() {
        int n = p.size();
        int i, j = n - 1;
        multiple = new float[n];
        constant = new float[n];
        for (i=0; i < n; i++) {
            Vec2 vi = p.get(i);
            Vec2 vj = p.get(j);
            multiple[i] = (vj.x - vi.x) / (vj.y - vi.y);
            constant[i] = vi.x - vi.y * multiple[i];
            j = i;
        }
    }
    
    /**
     * @param v
     * @return Vrati true, pokial sa vrchol nachadza v polygone. Treba mat
     *         predpocitane hodnoty primarneho polygonu metodou precalc_values().
     */
    private boolean pointInPolygon(Vec2 v) {
        float x = v.x;
        float y = v.y;
        int n = p.size();
        int i, j = n - 1;
        boolean b = false;
        for (i = 0; i < n; i++) {
            Vec2 vi = p.get(i);
            Vec2 vj = p.get(j);
            if ((vi.y < y && vj.y >= y || vj.y < y && vi.y >= y) && y * multiple[i] + constant[i] < x) {
                b = !b;
            }
            j = i;
        }
        return b;
    }
}