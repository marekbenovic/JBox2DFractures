package org.jbox2d.fracture.materials;

import java.util.Random;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.Material;

/**
 * Material simulujuci sklo. Ohniska su generovane nahodne v n-kruzniciach s
 * malou odchylkou.
 *
 * @author Marek Benovic
 */
public class Glass extends Material {
    private final Random r = new Random();

    /**
     * Pocet prvkov kruznice.
     */
    public int levels = 4;
    
    /**
     * Pocet prvkov v jednej kruznici.
     */
    public int count = 30;
    
    /**
     * Konstruktor inicializujuci sklo.
     */
    public Glass() {
        m_shattering = 1.0f;
        m_radius = 4f;
    }
    
    @Override
    public Vec2[] focee(Vec2 startPoint, Vec2 vektor) {
        Transform t = new Transform();
        t.set(startPoint, 0);

        int allCount = count * levels;
        
        Vec2[] va = new Vec2[allCount];
        for (int l = 0; l < levels; l++) {
            for (int c = 0; c < count; c++) {
                int i = l * count + c;
                
                double u = r.nextDouble() * Math.PI * 2; // uhol pod ktorym sa nachadza dany bod
                double deficit = (r.nextDouble() - 0.5) * m_shattering / 20;
                double r = (l + 1) * m_shattering + deficit;
                
                double x = Math.sin(u) * r;
                double y = Math.cos(u) * r;

                Vec2 v = new Vec2((float)x, (float)y);

                va[i] = Transform.mul(t, v);
            }
        }
        
        return va;
    }
    
    @Override
    public String toString() {
        return "Glass";
    }
}