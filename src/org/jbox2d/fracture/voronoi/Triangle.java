package org.jbox2d.fracture.voronoi;

import org.jbox2d.common.Vec2;

/**
 * Trojuholnik delaunay triangulacie
 *
 * @author Marek Benovic
 */
public class Triangle {
    /**
     * Index bodu trojuholnika
     */
    public int i, j, k;
    
    /**
     * Ak je nastavena referencia, ukazuje na trojuholnik, ktory ma rovnaky focus (v Vec2 formate). focusCorelation.focusCorelation je vzdy null.
     */
    public Triangle focusCorelation; //trojuholnik s fuplicitnym stredom opisanej kruznice (vzdy vedie ku korenovemu trojuholniku)
    
    /**
     * Suradnice stredu opisanej kruznice.
     */
    public double dX, dY;
    
    int index; //index trojuholnika
    double r; //polomer^2 - ak je positive.infinity alebo NaN() -> vyuzije funkcia Aritmetic.Site() na zistenie, ci bod lezi v kruznici, alebo nie

    Triangle(int index) {
        this.index = index;
    }
   
    /**
     * Inicializuje trojuholnik
     * @param i 1. index vrcholu
     * @param j 2. index vrcholu
     * @param k 3. index vrcholu
     * @param p Pole vrcholov
     * @param t protilahly trojuholnik
     */
    final void init(final int i, final int j, final int k, final Vec2[] p, final Triangle t) {
        this.i = i;
        this.j = j;
        this.k = k;
        
        //nastavi stred opisanej kruznice
        Vec2 a = p[i];
        Vec2 b = p[j];
        Vec2 c = p[k];
        
        double Bx = (double) b.x - a.x;
        double By = (double) b.y - a.y;
        double Cx = (double) c.x - a.x;
        double Cy = (double) c.y - a.y;
        double D = 1.0 / (2 * (Bx * Cy - By * Cx)); //overene - padne to len ak su body uuuuplne identicke, ak niesu, Bx | By a Cx | Cy != 0 a D musi byt rozumne cislo
        double Bs = Bx * Bx + By * By;
        double Cs = Cx * Cx + Cy * Cy;
        double x = (Cy * Bs - By * Cs) * D;
        double y = (Bx * Cs - Cx * Bs) * D;
        dX = x + a.x;
        dY = y + a.y;

        //nastavi polomer^2 opisanej kruznice
        //polomer treba trochu znizit kvoli probleme zaokruhlovania (aritmetika ho defaultne robi o epsilon vacsi, ako je treba, co sposobuje pri extremnych vstupoch exception)
        r = (x * x + y * y) * (1.0 - 1E-15);

        //nastavi ohisko, pokial sa zhoduje suradnicami s ohniskom protilahleho trojuholnika, tak pouzije jeho index
        if (t != null && (float) dX == (float) t.dX && (float) dY == (float) t.dY) {
            focusCorelation = t.focusCorelation != null ? t.focusCorelation : t; //podmienka zarucuje, ze focusCorelation vzdy vedie ku korenovemu trianglu
        } else {
            focusCorelation = null;
        }
    }
    
    /**
     * @param I
     * @param J
     * @return Vrati index toho vrcholu, ktory nieje v niektorom z parametrov
     */
    final int get(final int I, final int J) {
        return i == I ? j == J ? k : j : i == J ? j == I ? k : j : i;
    }
    
    /**
     * @param v
     * @return Vrati true, ak sa bod v nachadza vo vnutri opisanej kruznice
     */
    final boolean inside(final Vec2 v) {
        return dis(v) < r;
    }
    
    private double dis(final Vec2 v) {
        double x = dX - v.x;
        double y = dY - v.y;
        return x * x + y * y;
    }
}