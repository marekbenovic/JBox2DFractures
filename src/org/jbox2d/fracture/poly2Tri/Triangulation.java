package org.jbox2d.fracture.poly2Tri;

import java.util.ArrayList;
import org.jbox2d.common.Vec2;

public class Triangulation {
    /**
     * Set this to TRUE to obtain file with name debugFileName with the
     * log about triangulation
     */
    public static boolean debug = false;

    /**
     * If debug == true file with this name will be created during triangulation.
     */
    public static String debugFileName = "polygon_triangulation_log.txt";

    /**
     * Vola funkciu trangulate na polygone bez dier. Vracia rovnaku hodnotu
     * ako funkcia triangulate(int, int[], double[][])
     * 
     * @param vertices
     * @param count
     * @return 
     */
    public static ArrayList triangulate(Vec2[] vertices, int count) {
        return triangulate(1, new int[] { count }, vertices);
    }

    /**
      * numContures == number of contures of polygon (1 OUTER + n INNER)
      * numVerticesInContures == array numVerticesInContures[x] == number of vertices in x. contures, 0-based
      * vertices == array of vertices, each item of array contains doubl[2] ~ {x,y}
      * First conture is OUTER -> vertices must be COUNTER CLOCKWISE!
      * Other contures must be INNER -> vertices must be CLOCKWISE!
      * Example:
      * 	numContures = 1 (1 OUTER CONTURE, 1 INNER CONTURE)
      * 	numVerticesInContures = { 3, 3 } // triangle with inner triangle as a hol
      *     vertices = { {0, 0}, {7, 0}, {4, 4}, // outer conture, counter clockwise order
      *                  {2, 2}, {2, 3}, {3, 3}  // inner conture, clockwise order
      *     		   }
      *     
      * If error occurs during triangulation, null is returned.
      *     
      * @param numContures number of contures of polygon (1 OUTER + n INNER)
      * @param numVerticesInContures array numVerticesInContures[x] == number of vertices in x. contures, 0-based
      * @param vertices array of vertices, each item of array contains doubl[2] ~ {x,y}
      * @return ArrayList of ArrayLists which are triangles in form of indexes into array vertices
      */
    public static ArrayList triangulate(int numContures, int[] numVerticesInContures, Vec2[] vertices) {
            Polygon p = new Polygon(numContures, numVerticesInContures, vertices);
            if (debug){
                    p.setDebugFile(debugFileName);
                    p.setDebugOption(debug);
            } else {
                    p.setDebugOption(false);
            }
            if (p.triangulation())
                    return p.triangles();
            else
                    return null;
    }
}
