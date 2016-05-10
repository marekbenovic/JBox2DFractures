package org.jbox2d.fracture.materials;

import java.util.Random;
import org.jbox2d.common.Vec2;
import org.jbox2d.fracture.Material;
import org.jbox2d.fracture.util.MyList;

/**
 * Material, kde ohniska fragmentov su generovane rovnomerne nahodne v priestore.
 *
 * @author Marek Benovic
 */
public class Uniform extends Material {
    private Random r = new Random();
    
    @Override
    public Vec2[] focee(Vec2 bodNarazu, Vec2 vektorNarazu) {
        MyList<Vec2> focee = new MyList<>();
        float rad = m_shattering / 4;
        for (int i = 0; i < 100; ++i) {
            float x = r.nextFloat() - 0.5f;
            float y = r.nextFloat() - 0.5f;
            
            x = x * rad * 10;
            y = y * rad * 10;

            Vec2 v = new Vec2(x, y);
            
            v.addLocal(bodNarazu);
            
            focee.add(v);
        }
        Vec2[] foceeArray = new Vec2[focee.size()];
        focee.addToArray(foceeArray);
        return foceeArray;
    }
    
    @Override
    public String toString() {
        return "Uniform diffusion";
    }
}