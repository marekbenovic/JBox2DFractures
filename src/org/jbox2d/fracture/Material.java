package org.jbox2d.fracture;

import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.fragmentation.Singleton;
import org.jbox2d.fracture.fragmentation.IContains;
import org.jbox2d.fracture.materials.*;

/**
 * Material telesa
 *
 * @author Marek Benovic
 */
public abstract class Material {
    /**
     * Singleton sluziaci na fragmentaciu.
     */
    protected static final Singleton geom = new Singleton();
    
    /**
     * Najmensi ulomok, ktory je mozne triestit - aby sa zabranilo rekurzivnemu triesteniu.
     */
    public static final float MINMASSDESCTRUCTION = 0.5f;
    
    /**
     * Po destrukcii kruhu je kruh transformovany na regular polygon s danym poctom vrcholov.
     */
    public static final int CIRCLEVERTICES = 32;
    
    /**
     * Objekty musia mat najdlhsiu hranu (radius) vacsiu ako dany limit, vacsi obsah
     * a taktiez mass / radius.
     */
    public static final double MINFRAGMENTSIZE = 0.01;
    
    /**
     * Material polárneho logaritmického rozptylu
     */
    public static final Material DIFFUSION = new Diffusion();
    
    /**
     * Materiál rovnomerného rozptylu
     */
    public static final Material UNIFORM = new Uniform();
    
    /**
     * Sklo
     */
    public static final Material GLASS = new Glass();

    /**
     * Od akeho limitu tangentInertia sa zacne objekt triestit.
     */
    public float m_rigidity = 64.0f;
    
    /**
     * Na ake drobne kusky sa objekt zvykne triestit (minimalne). Sluzi pre
     * material na urcovanie, do akej vzdialenosti budu fragmenty sucastou
     * povodneho telesa a ktore sa odstiepia. Polomer na ^2
     */
    public float m_shattering = 4.0f;
    
    /**
     * Polomer kruhu, z ktoreho sa rataju fragmenty (fragmenty mimo kruhu su
     * zjednocovane do povodneho telesa)
     */
    public float m_radius = 2.0f;
    
    /**
     * Aky material sa definuje na fragmenty. this - fragmenty preberaju material
     * od povodneho predka, null - ziadne rekurzivne triestenie. Pomocou inych
     * referencii sa da dobre definovat napr. cihlova stena.
     */
    public Material m_fragments = this;
    
    /**
     * Abstraktna funkcia urcujuca sposob triesenia.
     * 
     * @param contactVector
     * @param contactPoint
     * @return Vrati ohniska v ktorych sa bude teleso triestit.
     */
    protected abstract Vec2[] focee(Vec2 contactPoint, Vec2 contactVector);
    
    /**
     * Fragmentacia telesa.
     * @param p Teleso
     * @param vektor Vektor ratajuc aj jeho velkost - ta urcuje rychlost.
     * @param bodNarazu Lokalny bod narazu na danom telese.
     * @param normalImpulse Intenzita kolizie
     * @return Vrati pole Polygonov, na ktore bude dany polygon rozdeleny
     */
    protected Polygon[] split(Polygon p, Vec2 bodNarazu, Vec2 vektor, float normalImpulse) {
        Vec2[] foceeArray = focee(bodNarazu, vektor);

        geom.calculate(p, foceeArray, bodNarazu, new IContains() {
            @Override
            public boolean contains(Vec2 point) {
                float x = bodNarazu.x - point.x;
                float y = bodNarazu.y - point.y;

                float xx = x;
                float yy = y;

                //inverzna tranformacia
                float ln = vektor.length();
                float sin = -vektor.x / ln;
                float cos = -vektor.y / ln;
                x = cos * xx + -sin * yy;
                y = sin * xx + cos * yy;
                
                //definicia filtru posobnosti
                float r = m_radius; // polomer
                float c = 2;
                float d = ln * c;
                d = Math.max(d, r);
                
                r = r * r;
                d = d * d;

                return (x * x + y * y < r && y < 0) || (x * x / r + y * y / d < 1 && y > 0);
            }
        });

        return geom.fragments;
    }
}
