package org.jbox2d.fracture.materials;

import java.util.Random;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.Material;

/**
 * Material simulujuci logaritmicky rozptyl - zhustene ohniska pri dotykovom bode
 * a nizsia koncentracia vo vacsej vzdialenosti s limitou v nekonecne. Zohladneny
 * je aj kolizny vektor.
 *
 * @author Marek Benovic
 */
public class Diffusion extends Material {
    private Random r = new Random();
    
    @Override
    public Vec2[] focee(Vec2 startPoint, Vec2 vektor) {
        final int count = 128; //pocet
        double c = 4; // natiahnutie

        vektor = new Vec2(1, 0);

        float ln = vektor.length();
        Transform t = new Transform();
        t.set(startPoint, 0);
        t.q.c = vektor.y / ln;
        t.q.s = vektor.x / ln;
        
        Vec2[] va = new Vec2[count];
        for (int i = 1; i <= count; i++) {
            
            double a = r.nextDouble() * 2 * Math.PI;
            double d = -Math.log(r.nextDouble()) * m_shattering;

            double x = Math.sin(a) * d;
            double y = Math.cos(a) * d * c;
            
            Vec2 v = new Vec2((float)x, (float)y);

            va[i - 1] = Transform.mul(t, v);
        }
        
        return va;
    }
    
    @Override
    public String toString() {
        return "Logaritmic diffusion";
    }
}