package org.jbox2d.fracture.fragmentation;

import org.jbox2d.common.Vec2;
import java.awt.geom.Point2D;

/**
 * Implementuje aritmeticke funkcie potrebne pre vypocet fragmentacie.
 *
 * @author Marek Benovic
 */
public class Arithmetic {
    private static final double DEFICIT = 0.0001; //odchylka, v rozmedzi ktorej sa rataju 3 body ako leziace na 1 linii
    
    private Arithmetic() {
    }
    
    /**
     * @param a
     * @param b
     * @return Vrati kvadraticku vzdialenost bodov z parametra.
     */
    public static double distanceSq(Vec2 a, Vec2 b) {
        double x = a.x - b.x;
        double y = a.y - b.y;
        return x * x + y * y;
    }
    
    /**
     * @param a
     * @param b
     * @return Vrati kvadraticku vzdialenost 2 bodov z parametra.
     */
    public static double distanceSq(Vec2 a, Point2D.Double b) {
        double x = a.x - b.x;
        double y = a.y - b.y;
        return x * x + y * y;
    }
    
    /**
     * @param a
     * @param b
     * 
     * @return Kvadraticky uhol v rozmedzi (0-4) medzi vektorom (b - a) a vektorom (0, 1).
     */
    public static double angle(Vec2 a, Vec2 b) {
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double x = vx * vx;
        double cos = x / (x + vy * vy); //neni to linearne vzhladom na uzol - kvoli optimalizacii sa odstranila odmocnina, ale to nevadi
        return vx > 0 ? vy > 0 ? 3 + cos : 1 - cos : vy > 0 ? 3 - cos : 1 + cos;
    }
    
    /**
     * @param a 1. bod usecky
     * @param b 2. bod usecky
     * @param v Bod, u ktoreho sa rozhoduje, na ktorej strane sa nachadza.
     * @return <tt>-1</tt>, ak sa bod <tt>v</tt> nachadza na lavo od usecky |ab|<br>
     *         <tt>0</tt>, ak body <tt>a, b, v</tt> lezia na jednej priamke.<br>
     *         <tt>1</tt>, ak sa bod <tt>v</tt> nachadza na pravo od usecky |ab|<br>
     */
    public static int site(Vec2 a, Vec2 b, Vec2 v) {
        double g = (b.x - a.x) * (v.y - b.y);
        double h = (v.x - b.x) * (b.y - a.y);
        return g > h ? 1 : g == h ? 0 : -1;
    }
    
    /**
     * @param a 1. bod usecky
     * @param b 2. bod usecky
     * @param v Bod, u ktoreho sa rozhoduje, na ktorej strane sa nachadza.
     * @return Rovnako ako funkcia site, s tym rozdielom, ze zohladnuje deficit.
     */
    public static int siteDef(Vec2 a, Vec2 b, Vec2 v) {
        double ux = b.x - a.x;
        double uy = b.y - a.y;
        double wx = b.x - v.x;
        double wy = b.y - v.y;
        double sin = (ux * wy - wx * uy) / Math.sqrt((ux * ux + uy * uy) * (wx * wx + wy * wy));
        if (Double.isNaN(sin) || Math.abs(sin) < DEFICIT) {
            return 0;
        }
        return sin < 0 ? 1 : -1;
    }
}