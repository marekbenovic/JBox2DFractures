package org.jbox2d.fracture;

import java.util.ArrayList;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.fracture.util.MyList;

import static org.jbox2d.fracture.Material.CIRCLEVERTICES;

/**
 * Objekt, ktory reprezentuje rozbitie jedneho telesa.
 *
 * @author Marek Benovic
 */
public class Fracture {
    /**
     * Normal Impulse, ktory vyvolal frakturu
     */
    public final float normalImpulse;

    private final Fixture f1; //primarna fixture, ktora sa rozbija
    private final Fixture f2; //sekundarna fixture
    private final Body b1; //telo fixtury f1
    private final Body b2; //telo fuxtury f2
    private final Material m; //material triestiaceho sa telesa
    private final Vec2 point; //kolizny bod vo worldCoordinates
    private final Contact contact; //kontakt, z ktoreho vznika fraktura
    
    /**
     * Vytvori Frakturu. Ta este nieje aplikovana na svet.
     */
    private Fracture(Fixture f1, Fixture f2, Material m, Contact contact, float normalImpulse, Vec2 point) {
        this.f1 = f1;
        this.f2 = f2;
        this.b1 = f1.m_body;
        this.b2 = f2.m_body;
        this.m = m;
        this.point = point;
        this.contact = contact;
        this.normalImpulse = normalImpulse;
    }
    
    /**
     * Rozbije objekt. Upravi objekt world tak, ze vymaze triesteny objekt
     * a nahradi ho fragmentami na zaklade nastaveneho materialu a clenskych
     * premennych.
     * @param dt casova dlzka framu
     */
    public void smash(float dt) {
        if (contact == null) { //riesi sa staticky prvok, ktory ma priliz maly obsah
            b1.setType(BodyType.DYNAMIC);
            return;
        }
        
        World w = b1.m_world;
        Shape s = f1.m_shape;
        Polygon p = f1.m_polygon;

        if (p == null) {
            switch (s.m_type) {
                case POLYGON:
                    PolygonShape ps = (PolygonShape) s;
                    Vec2[] vertices = ps.m_vertices;
                    p = new Polygon();
                    for (int i = 0; i < ps.m_count; ++i) {
                        p.add(vertices[ps.m_count - i - 1]);
                    }
                    break;
                case CIRCLE:
                    CircleShape cs = (CircleShape) s;
                    p = new Polygon();
                    float radius = cs.m_radius;

                    double u = Math.PI * 2 / CIRCLEVERTICES;
                    radius = (float) Math.sqrt(u / Math.sin(u)) * radius; //upravim radius tak, aby bola zachovana velkost obsahu

                    Vec2 center = cs.m_p;
                    for (int i = 0; i < CIRCLEVERTICES; ++i) {
                        double j = u * i; //uhol
                        float sin = (float) Math.sin(j);
                        float cos = (float) Math.cos(j);
                        Vec2 v = new Vec2(sin, cos).mulLocal(radius).addLocal(center);
                        p.add(v);
                    }
                    break;
                default:
                    throw new RuntimeException("Dany typ tvaru nepodporuje stiepenie");
            }
        }
        
        float mConst = f1.m_material.m_rigidity / normalImpulse; //sila v zavislosti na pevnosti telesa
        
        boolean fixA = f1 == contact.m_fixtureA; //true, ak f2 je v objekte contact ako m_fixtureA
        float oldAngularVelocity = fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
        Vec2 oldLinearVelocity = fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
        b1.setAngularVelocity((b1.m_angularVelocity - oldAngularVelocity) * mConst + oldAngularVelocity);
        b1.setLinearVelocity(b1.m_linearVelocity.sub(oldLinearVelocity).mulLocal(mConst).addLocal(oldLinearVelocity));
        if (!w.isFractured(f2) && b2.m_type == BodyType.DYNAMIC && !b2.m_fractureTransformUpdate) { //ak sa druhy objekt nerozbija, tak sa jej nahodia povodne hodnoty (TREBA MODIFIKOVAT POHYB OBJEKTU, KTORY SPOSOBUJE ROZPAD)
            oldAngularVelocity = !fixA ? contact.m_angularVelocity_bodyA : contact.m_angularVelocity_bodyB;
            oldLinearVelocity = !fixA ? contact.m_linearVelocity_bodyA : contact.m_linearVelocity_bodyB;
            b2.setAngularVelocity((b2.m_angularVelocity - oldAngularVelocity) * mConst + oldAngularVelocity);
            b2.setLinearVelocity(b2.m_linearVelocity.sub(oldLinearVelocity).mulLocal(mConst).addLocal(oldLinearVelocity));
            b2.setTransform(b2.m_xf0.p.add(b2.m_linearVelocity.mul(dt)), b2.m_xf0.q.getAngle()); //osetruje jbox2d od posuvania telesa pri rieseni kolizie
            b2.m_fractureTransformUpdate = true;
        }
        
        Vec2 localPoint = Transform.mulTrans(b1.m_xf, point);
        Vec2 b1Vec = b1.getLinearVelocityFromWorldPoint(point);
        Vec2 b2Vec = b2.getLinearVelocityFromWorldPoint(point);
        Vec2 localVector = b2Vec.subLocal(b1Vec);
        
        localVector.mulLocal(dt);
        Polygon[] fragment;
        try {
            fragment = m.split(p, localPoint, localVector, normalImpulse); //rodeli to
        } catch (RuntimeException ex) {
            return;
        }
        
        if (fragment.length == 1) { //nerozbilo to na ziadne fragmenty
            return;
        }
        
        //definuje tela fragmentov - tie maju vsetky rovnaku definiciu (preberaju parametre z povodneho objektu)
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(b1.m_xf.p); //pozicia
        bodyDef.angle = b1.m_xf.q.getAngle(); // otocenie
        bodyDef.fixedRotation = b1.isFixedRotation();
        bodyDef.angularDamping = b1.m_angularDamping;
        bodyDef.allowSleep = b1.isSleepingAllowed();
        
        FixtureDef fd = new FixtureDef();
        fd.friction = f1.m_friction; // trenie
        fd.restitution = f1.m_restitution; //odrazivost
        fd.isSensor = f1.m_isSensor;
        fd.density = f1.m_density;
        
        //odstrani fragmentacne predmety/cele teleso
        ArrayList<Fixture> fixtures = new ArrayList<>();
        if (f1.m_polygon != null) {
            for (Fixture f = b1.m_fixtureList; f != null; f = f.m_next) {
                if (f.m_polygon == f1.m_polygon) {
                    fixtures.add(f);
                }
            }
        } else {
            fixtures.add(f1);
        }
        
        for (Fixture f : fixtures) {
            b1.destroyFixture(f);
        }
        
        if (b1.m_fixtureCount == 0) {
            w.destroyBody(b1);
        }

        //prida fragmenty do simulacie
        MyList<Body> newbodies = new MyList<>();
        for (Polygon pg : fragment) { //vytvori tela, prida fixtury, poriesi konvexnu dekompoziciu
            if (pg.isCorrect()) {
                if (pg instanceof Fragment) {
                    Polygon[] convex = pg.convexDecomposition();
                    bodyDef.type = BodyType.DYNAMIC;
                    for (Polygon pgx : convex) {
                        Body f_body = w.createBody(bodyDef);
                        pgx.flip();
                        PolygonShape ps = new PolygonShape();
                        ps.set(pgx.getArray(), pgx.size());
                        fd.shape = ps;
                        fd.polygon = null;
                        fd.material = f1.m_material.m_fragments; //rekurzivne stiepenie

                        f_body.createFixture(fd);
                        f_body.setAngularVelocity(b1.m_angularVelocity);
                        f_body.setLinearVelocity(b1.getLinearVelocityFromLocalPoint(f_body.getLocalCenter()));
                        newbodies.add(f_body);
                    }

                } else {
                    fd.material = f1.m_material.m_fragments; //rekurzivne stiepenie
                    bodyDef.type = b1.getType();
                    Body f_body = w.createBody(bodyDef);
                    PolygonFixture pf = new PolygonFixture(pg);

                    f_body.createFixture(pf, fd);
                    f_body.setLinearVelocity(b1.getLinearVelocityFromLocalPoint(f_body.getLocalCenter()));
                    f_body.setAngularVelocity(b1.m_angularVelocity);
                    newbodies.add(f_body);
                }
            }
        }

        //zavola sa funkcia z fraction listeneru (pokial je nadefinovany)
        FractureListener fl = w.getContactManager().m_fractureListener;
        if (fl != null) {
            fl.action(m, normalImpulse, newbodies);
        }
    }

    /**
     * Detekuje, ci dany kontakt vytvara frakturu
     * 
     * @param contact
     * @param impulse
     * @param w
     */
    public static void init(Contact contact, ContactImpulse impulse, World w) {
        Fixture f1 = contact.m_fixtureA;
        Fixture f2 = contact.m_fixtureB;
        float[] impulses = impulse.normalImpulses;
        for (int i = 0; i < impulse.count; ++i) {
            float iml = impulses[i];
            fractureCheck(f1, f2, iml, w, contact, i);
            fractureCheck(f2, f1, iml, w, contact, i);
        }
    }
    
    /**
     * Kontrolue, ci je kolizia kriticka, ak je, tak ju prida do hashovacej tabulky
     * kritickych kolizii. Treba zauvazit aj multimaterialove telesa. Materialy
     * tvoria spojovy zoznam, pre triestenie sa vsak pouzije len jeden
     */
    private static final MyList<Material> materials = new MyList<>();
    private static void fractureCheck(final Fixture f1, final Fixture f2, final float iml, World w, Contact contact, int i) {
        materials.clear();
        for (Material m = f1.m_material; m != null; m = m.m_fragments) {
            if (materials.contains(m)) {
                return;
            }
            if (m.m_rigidity < iml) {
                f1.m_body.m_fractureTransformUpdate = f2.m_body.m_fractureTransformUpdate = false;
                if (f1.m_body.m_massArea > Material.MINMASSDESCTRUCTION) {
                    WorldManifold wm = new WorldManifold();
                    contact.getWorldManifold(wm); //vola sa iba raz
                    w.addFracture(new Fracture(f1, f2, m, contact, iml, new Vec2(wm.points[i])));
                } else if (f1.m_body.m_type != BodyType.DYNAMIC) {
                    w.addFracture(new Fracture(f1, f2, m, null, 0, null));
                }
            }
            materials.add(m);
        }
    }
    
    private static boolean equals(Fixture f1, Fixture f2) {
        PolygonFixture p1 = f1.m_polygon;
        PolygonFixture p2 = f2.m_polygon;
        if (p1 != null && p2 != null) {
            return p1 == p2;
        } else {
            return f1 == f2;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fracture) {
            Fracture f = (Fracture) obj;
            return equals(f.f1, f1);
        } else if (obj instanceof Fixture) {
            Fixture f = (Fixture) obj;
            return equals(f, f1);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if (f1.m_polygon != null) {
            return f1.m_polygon.hashCode();
        } else {
            return f1.hashCode();
        }
    }
}
