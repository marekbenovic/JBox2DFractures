package org.gui.testbed;

import org.gui.ICase;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.fracture.Material;

/**
 * Testovaci scenar.
 *
 * @author Marek Benovic
 */
public class MainScene implements ICase {
    @Override
    public void init(World w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(0.0f, 5.0f); //pozicia
            Body newBody = w.createBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(5.0f, 5.0f);
            Fixture f = newBody.createFixture(shape3, 1.0f);
            f.m_friction = 0.2f; // trenie
            f.m_restitution = 0.0f; //odrazivost
            
            f.m_material = Material.UNIFORM;
            f.m_material.m_rigidity = 40.0f;
            f.m_material.m_shattering = 3.0f;
        }
        
        {
            BodyDef bodyDefBullet = new BodyDef();
            bodyDefBullet.type = BodyType.DYNAMIC;
            bodyDefBullet.position.set(-30.0f, 5.3f); //pozicia
            bodyDefBullet.linearVelocity = new Vec2(100.0f, 0.0f); // smer pohybu
            Body bodyBullet = w.createBody(bodyDefBullet);
            CircleShape circleShape = new CircleShape();
            circleShape.m_radius = 1.0f;
            Fixture fixtureBullet = bodyBullet.createFixture(circleShape, 5.0f);
            fixtureBullet.m_friction = 0.4f; // trenie
            fixtureBullet.m_restitution = 0.1f; //odrazivost
        }
    }
    
    @Override
    public String toString() {
        return "Main scene";
    }
}
