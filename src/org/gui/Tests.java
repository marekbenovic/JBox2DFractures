package org.gui;

import org.gui.testbed.Materials;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.gui.testbed.*;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.DistanceJoint;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import org.jbox2d.fracture.Material;
import org.jbox2d.fracture.Polygon;
import org.jbox2d.fracture.util.MyList;

/**
 * GUI pre testovacie scenare. Ovladanie:
 * s - start
 * r - reset
 * koliecko mysi - priblizovanie/vzdalovanie
 * prave tlacitko mysi - posuvanie sceny
 * lave tlacitko mysi - hybanie objektami
 *
 * @author Marek Benovic
 */
public class Tests extends JComponent implements Runnable {
    private final Dimension screenSize = new Dimension(1024, 540);
    private final Vec2 center = new Vec2();
    private float zoom = 1;
    private volatile World w;

    private volatile Vec2 startCenter = new Vec2();
    private volatile Point clickedPoint = null;
    private volatile Graphics2D g;
    private volatile boolean running = false;
    private volatile ICase testCase;

    private volatile MouseJointDef mjdef;
    private volatile MouseJoint mj;
    private volatile boolean destroyMj = false;
        
    private volatile Body ground;
    
    private volatile Vec2 mousePosition = new Vec2();
    
    /**
     * Pole testovacich scenarov
     */
    private static ICase[] cases = new ICase[] {
        new MainScene(),
        new Cube(),
        new Circle(),
        new RotatedBody(),
        new StaticBody(),
        new Fluid(),
        new Materials(Material.UNIFORM),
        new Materials(Material.DIFFUSION),
        new Materials(Material.GLASS)
    };
    
    private Tests(ICase testcase) {
    }
    
    private Tests() {
        initWorld();

        addMouseWheelListener((MouseWheelEvent e) -> {
            if (e.getWheelRotation() < 0) {
                zoom *= 1.5f * -e.getWheelRotation();
            } else {
                zoom /= 1.5f * e.getWheelRotation();
            }
            
            zoom = Math.min(zoom, 100);
            zoom = Math.max(zoom, 0.1f);
            repaint();
        });
        
        addMouseMotionListener(new MouseMotionListener(){
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                mousePosition = getPoint(p);
                if (clickedPoint != null) {
                    p.x -= clickedPoint.x;
                    p.y -= clickedPoint.y;
                    center.x = startCenter.x - p.x / zoom;
                    center.y = startCenter.y + p.y / zoom;
                } else {
                    if (mj != null) {
                        mj.setTarget(mousePosition);
                    }
                }
                if (!running) {
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                mousePosition = getPoint(p);
                if (!running) {
                    repaint();
                }
            }
        });
        
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                Point p = new Point(x, y);
                switch (e.getButton()) {
                    case 3:
                        startCenter.set(center);
                        clickedPoint = p;
                        break;
                    case 1:
                        Vec2 v = getPoint(p);
                        synchronized(Tests.this) {
                            bodyFor: for (Body b = w.getBodyList(); b != null; b = b.m_next) {
                                for (Fixture f = b.getFixtureList(); f != null; f = f.m_next) {
                                    if (f.testPoint(v)) {
                                        MouseJointDef def = new MouseJointDef();

                                        def.bodyA = ground;
                                        def.bodyB = b;
                                        def.collideConnected = true;

                                        def.target.set(v);

                                        def.maxForce = 500f * b.getMass();
                                        def.dampingRatio = 0;

                                        mjdef = def;
                                        break bodyFor;
                                    }
                                }
                            }
                        }

                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                synchronized (Tests.this) {
                    switch (e.getButton()) {
                        case 3:
                            clickedPoint = null;
                            break;
                        case 1:
                            if (mj != null) {
                                destroyMj = true;
                            }
                            break;
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        
        zoom = 10;
        center.set(0, 7);
    }
    
    private void initWorld() {
        w = new World(new Vec2(0, -9.81f));
        w.setParticleRadius(0.2f);
        w.setParticleDensity(1.0f);
        w.setContinuousPhysics(true);
        
        setBox();
        
        mj = null;
        destroyMj = false;
        mjdef = null;
    }
    
    private void setBox() {
        {
            BodyDef bd = new BodyDef();
            bd.setType(BodyType.STATIC);
            ground = w.createBody(bd);
            Body wallRight = w.createBody(bd);
            Body wallLeft = w.createBody(bd);

            PolygonShape shape1 = new PolygonShape();
            shape1.setAsBox(40, 5);
            ground.createFixture(shape1, 0.0f);
            ground.setTransform(new Vec2(0, -5.0f), 0);

            PolygonShape shape2r = new PolygonShape();
            shape2r.setAsBox(2, 40);
            wallRight.createFixture(shape2r, 0.0f);
            wallRight.setTransform(new Vec2(-41, 30.0f), 0);

            PolygonShape shape2l = new PolygonShape();
            shape2l.setAsBox(2, 40);
            wallLeft.createFixture(shape2l, 0.0f);
            wallLeft.setTransform(new Vec2(41, 30.0f), 0);
        }
    }
    
    private Point getPoint(Vec2 point) {
        float x = (point.x - center.x) * zoom + (getWidth() >> 1);
        float y = (getHeight() >> 1) - (point.y - center.y) * zoom;
        return new Point((int)x, (int)y);
    }
    
    private Vec2 getPoint(Point point) {
        float x = (point.x - (getWidth() >> 1)) / zoom + center.x;
        float y = ((getHeight() >> 1) - point.y) / zoom + center.y;
        return new Vec2(x, y);
    }
    
    private MyThread t = new MyThread();
    
    @Override
    public void run() {
        t.start();
    }
    
    private int stepsInSecond = 50;
    private int iterations = 8;
    private int velocity = 8;
    private int slowmotion = 1;
    private int plynuleSlowMo = 1;
    
    private MyThread createThread() {
        return new MyThread();
    }

    private synchronized void setCase(ICase testcase) {
        this.testCase = testcase;
        initWorld();
        testCase.init(w);
        repaint();
    }
    
    private class MyThread extends Thread {
        @Override
        public void run () {
            for (;;) {
                long l1 = System.nanoTime();
                synchronized(Tests.this) {
                    if (running) {
                        w.step(1.0f / stepsInSecond / plynuleSlowMo, velocity, iterations);
                    }
                    if (destroyMj) {
                        if (mj.getBodyA().m_fixtureCount > 0 && mj.getBodyB().m_fixtureCount > 0) {
                            w.destroyJoint(mj);
                        }
                        mj = null;
                        destroyMj = false;
                    }
                    if (mjdef != null && mj == null) {
                        mj = (MouseJoint) w.createJoint(mjdef);
                        mjdef.bodyA.setAwake(true);
                        mjdef = null;
                    }
                }

                repaint();
                
                long l3 = System.nanoTime();
                int fulltime = (int) ((double)(l3-l1) / 1000000);

                try {
                    int interval = (int) (1000.0f / stepsInSecond * slowmotion);
                    interval -= fulltime;
                    interval = Math.max(interval, 0);
                    Thread.sleep(interval);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    private void drawJoint(Joint joint) {
        g.setColor(Color.BLUE);
        Vec2 v1 = new Vec2();
        Vec2 v2 = new Vec2();
        switch (joint.getType()) {
            case DISTANCE:
                DistanceJoint dj = (DistanceJoint) joint;
                v1 = joint.getBodyA().getWorldPoint(dj.getLocalAnchorA());
                v2 = joint.getBodyB().getWorldPoint(dj.getLocalAnchorB());
                break;
            case MOUSE:
                MouseJoint localMj = (MouseJoint) joint;
                localMj.getAnchorA(v1);
                localMj.getAnchorB(v2);
                break;
        }
        Point p1 = getPoint(v1);
        Point p2 = getPoint(v2);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
    }
    
    private void drawParticles() {
        Vec2[] vec = w.getParticlePositionBuffer();
        if (vec != null) {
            g.setColor(Color.BLUE);
            float radius = w.getParticleRadius();
            int size = w.getParticleCount();
            for (int i = 0; i < size; i++) {
                Vec2 vx = vec[i];
                Point pp = getPoint(vx);
                float r = radius * zoom;
                int radInt = (int) (r * 2);
                if (radInt < 1) {
                    g.drawLine(pp.x, pp.y, pp.x, pp.y); //ak je zoom priliz maly, tak by kvapalinu nezobrazilo
                } else {
                    g.fillOval(pp.x - (int) r, pp.y - (int) r, (int)(r * 2), (int)(r * 2));
                }
            }
        }
    }
    
    private void drawBody(Body body) {
        if (body.getType() == BodyType.DYNAMIC) {
            g.setColor(Color.DARK_GRAY);
        } else {
            g.setColor(Color.GRAY);
        }
        Vec2 v = new Vec2();
        MyList<Polygon> generalPolygons = new MyList<>();
        for (Fixture f = body.m_fixtureList; f != null; f = f.m_next) {
            Polygon pg = f.m_polygon;
            if (pg != null) {
                if (!generalPolygons.contains(pg)) {
                    generalPolygons.add(pg);
                }
            } else {
                Shape shape = f.getShape();
                switch (shape.m_type) {
                    case POLYGON:
                        PolygonShape poly = (PolygonShape) shape;
                        int x[] = new int[poly.m_count];
                        int y[] = new int[poly.m_count];
                        for (int i = 0; i < poly.m_count; ++i) {
                            body.getWorldPointToOut(poly.m_vertices[i], v);
                            Point p = getPoint(v);
                            x[i] = p.x;
                            y[i] = p.y;
                        }
                        g.fillPolygon(x, y, poly.m_count);
                        break;
                    case CIRCLE:
                        CircleShape circle = (CircleShape) shape;
                        float r = circle.m_radius;
                        body.getWorldPointToOut(circle.m_p, v);
                        Point p = getPoint(v);
                        int wr = (int) (r * zoom);
                        g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
                        break;
                    case EDGE:
                        EdgeShape edge = (EdgeShape) shape;
                        Vec2 v1 = edge.m_vertex1;
                        Vec2 v2 = edge.m_vertex2;
                        Point p1 = getPoint(v1);
                        Point p2 = getPoint(v2);
                        g.drawLine(p1.x, p1.y, p2.x, p2.y);
                        break;
                }
            }
        }

        if (generalPolygons.size() != 0) {
            Polygon[] polygonArray = generalPolygons.toArray(new Polygon[0]);
            for (Polygon poly : polygonArray) {
                int n = poly.size();
                int x[] = new int[n];
                int y[] = new int[n];
                for (int i = 0; i < n; ++i) {
                    body.getWorldPointToOut(poly.get(i), v);
                    Point p = getPoint(v);
                    x[i] = p.x;
                    y[i] = p.y;
                }
                g.fillPolygon(x, y, n);
            }
        }
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        
        if (w == null) {
            return;
        }

        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) bi.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 

        //predpripravi scenu
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        int fixtures = 0;
        
        synchronized(this) {
            //vykresli particles
            drawParticles();
            
            //vykresli tuhe telesa
            for (Body b = w.getBodyList(); b != null; b = b.getNext()) {
                drawBody(b);
                fixtures += b.m_fixtureCount;
            }

            //vykresli joiny
            for (Joint j = w.getJointList(); j != null; j = j.m_next) {
                drawJoint(j);
            }
        }

        //text
        {
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("Courier New", Font.BOLD, 18));
            g.setColor(Color.BLACK);
            g.drawString("s - start/stop", 20, 20);
            g.drawString("r - reset", 20, 40);
            g.setColor(Color.BLACK);
            g.drawString("Mouse position:  ["+mousePosition.x+", "+mousePosition.y+"]", 20, 60);
            g.drawString("Screen position: ["+center.x+", "+center.y+"]", 20, 80);
            g.drawString("Zoom:      "+zoom, 20, 100);
            g.drawString("Bodies:    "+w.getBodyCount(), 20, 120);
            g.drawString("Fixtures:  "+fixtures, 20, 140);
            g.drawString("Contacts:  "+w.getContactCount(), 20, 160);
            g.drawString("Particles: "+w.getParticleCount(), 20, 180);
        }
        
        g.setFont(new Font("Courier New", Font.BOLD, 16));
        g.drawString("Marek Beňovič © 2015", 20, getHeight() - 20);
        
        graphics.drawImage(bi, 0, 0, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return screenSize;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    /**
     * Primarny kod spustajuci framework pre testovacie scenare.
     * @param args 
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Tests");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                Container pane = frame.getContentPane();
                
                pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

                String[] caseNames = new String[cases.length];
                for (int i = 0; i < cases.length; ++i) {
                    caseNames[i] = (i+1)+". "+cases[i].toString();
                }
                
                JComboBox petList = new JComboBox(caseNames);
                pane.add(petList);
                
                Dimension dimMax = petList.getMaximumSize();
                petList.setMaximumSize(new Dimension(dimMax.width, 30));
                
                Tests canvas = new Tests();
                
                petList.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JComboBox cb = (JComboBox) e.getSource();
                        int index = cb.getSelectedIndex();
                        canvas.setCase(cases[index]);
                        pane.requestFocusInWindow();
                    }
                });

                canvas.setAlignmentX(Component.CENTER_ALIGNMENT);
                pane.add(canvas);
                
                canvas.setCase(cases[0]);
                
                pane.setFocusable(true);
                pane.requestFocusInWindow();
                
                pane.addKeyListener(new KeyListener(){
                    @Override
                    public void keyTyped(KeyEvent e) {
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                        switch (e.getKeyChar()) {
                            case 's':
                                canvas.running = !canvas.running;
                                break;
                            case 'r':
                                try { //pockam, kym vlakno dobehne (robilo to nejake problemy s logami)
                                    canvas.t.interrupt();
                                    canvas.t.join();
                                } catch (InterruptedException ex) {
                                }
                                canvas.t = canvas.createThread();
                                canvas.initWorld();
                                canvas.testCase.init(canvas.w);
                                canvas.t.start();
                                break;
                        }
                        
                    }
                    @Override
                    public void keyReleased(KeyEvent e) {
                    }
                });
                
                canvas.run();
                
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}