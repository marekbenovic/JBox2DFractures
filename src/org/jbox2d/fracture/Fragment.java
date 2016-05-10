package org.jbox2d.fracture;

import org.jbox2d.fracture.fragmentation.Arithmetic;
import org.jbox2d.common.Vec2;

/**
 * Polygon pre voronoi diagram. Funguje ako ArrayList 2D bodov typu Point2D,
 * ktory potom zotriedim podla ohniska na konvexny polygon. V pripade potreby si
 * moze uzivatel dodefinovat dalsie funkcie - oddedit od polygonu a nadefinovat
 * si funkcie na vypocet obsahu, alebo taziska. Jedna sa o specificky pripad polygonu.
 * Reprezentuje uz konkretny ulomok povodneho telesa.
 *
 * @author Marek Benovic
 * 
 * @version 1.0
 */
public class Fragment extends Polygon {
    /**
     * Inicializuje prazdny fragment
     */
    public Fragment() {
    }
    
    /**
     * Inicializuje fragment na zaklade vstupneho pola (priradi referenciu).
     * @param ar 
     */
    public Fragment(Vec2[] ar) {
        super(ar);
    }
    
    /**
     * Ohnisko fragmentu
     */
    public Vec2 focus;
    
    /**
     * Pomocna premenna pre vypocet do geometry kniznice.
     */
    public boolean visited;
    
    /**
     * Zotriedi konvexny polygon podla bodu focus na zaklade uhlov jednotlivych
     * vrcholov
     */
    public void resort() {
        int size = size();
        double[] comparer = new double[size];
        for (int i = 0; i != size; ++i) {
            comparer[i] = Arithmetic.angle(get(i), focus);
        }
        for (int i = 0; i != size; ++i) {
            int maxIndex = i;
            for (int j = i + 1; j != size; ++j) {
                if (comparer[j] < comparer[maxIndex]) {
                    maxIndex = j;
                }
            }
            double swap = comparer[i];
            comparer[i] = comparer[maxIndex];
            comparer[maxIndex] = swap;
            swap(i, maxIndex);
        }
    }
    
    /**
     * Zotriedi vrcholy polygonu do konvexneho polygonu, ako idu za sebou.
     * Triedi podla uhlu, aky zviera usecka tvoriaca bodmi focus a lubovolny
     * vrchol polygonu. Polygony su vacsinou velmi male, cca 8 bodov, preto
     * je vyuzivany selected sort ako najrychlejsi algoritmus na data takehoto
     * typu.
     * 
     * @param focus Vlozi vnutorny bod, podla ktoreho zotriedi polygon - podla
     *              uhlu spojnice daneho bodu a parametra.
     */
    void sort(Vec2 focus) {
        this.focus = focus;
        resort();
    }
    
    /**
     * Vymeni 2 vrcholy polygonu
     * @param i
     * @param j 
     */
    private void swap(int i, int j) {
        Vec2 item = array[i];
        array[i] = array[j];
        array[j] = item;
    }
}
