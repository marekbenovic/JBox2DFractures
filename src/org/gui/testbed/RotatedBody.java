package org.gui.testbed;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.gui.ICase;
import org.jbox2d.fracture.materials.*;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class RotatedBody implements ICase {
    @Override
    public void init(World w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(10.0f, 20.0f); //pozicia
            bodyDef2.linearVelocity = new Vec2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 10.0f; //rotacia (rychlost rotacie)
            Body newBody = w.createBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(1.0f, 10.0f);

            Fixture f = newBody.createFixture(shape3, 1.0f);
            f.m_friction = 0.2f; // trenie
            f.m_restitution = 0.0f; //odrazivost
            f.m_material = new Diffusion();
            f.m_material.m_rigidity = 32.0f;
        }
    }
    
    @Override
    public String toString() {
        return "Rotated body";
    }
}
