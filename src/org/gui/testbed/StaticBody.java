package org.gui.testbed;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.fracture.PolygonFixture;
import org.gui.ICase;
import org.jbox2d.fracture.materials.Diffusion;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class StaticBody implements ICase {
    @Override
    public void init(World w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.STATIC;
            bodyDef2.position.set(10.0f, 0.0f); //pozicia
            bodyDef2.linearVelocity = new Vec2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body newBody = w.createBody(bodyDef2);

            PolygonFixture pf = new PolygonFixture(new Vec2[]{
                new Vec2(0.0f,3.7f),
                new Vec2(6.3f,3.7f),
                new Vec2(6.3f,16.0f),
                new Vec2(3.8f,16.0f),
                new Vec2(3.7f,20.0f),
                new Vec2(8.2f,20.0f),
                new Vec2(8.2f,0.0f),
                new Vec2(0.0f,0.0f)
            });
            FixtureDef fd = new FixtureDef();
            fd.friction = 0.2f; // trenie
            fd.restitution = 0.0f; //odrazivost
            fd.density = 1.0f;
            fd.material = new Diffusion();
            fd.material.m_rigidity = 32.0f;
            newBody.createFixture(pf, fd);
        }

        {
            BodyDef bodyDefBullet = new BodyDef();
            bodyDefBullet.type = BodyType.DYNAMIC;
            bodyDefBullet.position.set(-20.0f, 18.0f); //pozicia
            bodyDefBullet.linearVelocity = new Vec2(100.0f, 0.0f); // smer pohybu
            bodyDefBullet.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body bodyBullet = w.createBody(bodyDefBullet);

            CircleShape circleShape = new CircleShape();
            circleShape.m_radius = 1.0f;
            Fixture fixtureBullet = bodyBullet.createFixture(circleShape, 2.0f);
            fixtureBullet.m_friction = 0.4f; // trenie
            fixtureBullet.m_restitution = 0.1f; //odrazivost
        }
    }
    
    @Override
    public String toString() {
        return "Static body";
    }
}
