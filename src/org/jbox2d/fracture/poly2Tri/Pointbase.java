package org.jbox2d.fracture.poly2Tri;

import org.jbox2d.fracture.poly2Tri.splayTree.BTreeNode;
import org.jbox2d.fracture.poly2Tri.splayTree.SplayTreeAction;
import org.jbox2d.fracture.poly2Tri.splayTree.SplayTreeItem;

public class Pointbase implements Comparable {

	/**
	 * id of point;
	 */
	public int      id = -1;              
	
	/**
	 * coordinates;
	 */
    public double   x = 0, y = 0;
    
    /**
     * type of points;
     */
    //Type            type;            
    public int 		type = Poly2TriUtils.UNKNOWN;
    
    /**
     * left chain or not;
     */
    public boolean  left = false;               
	
    public Pointbase(){   
    	
    }
    
    public Pointbase(Pointbase pb){
    	this.id=pb.id; 
    	this.x=pb.x;
    	this.y=pb.y;
    	this.type=pb.type;
    	this.left=pb.left;    	
    }
    
    public Pointbase(double xx, double yy){
    	x = xx; y = yy;
    }
    
    public Pointbase(int idd, double xx, double yy){
    	id = idd; x = xx; y = yy;
    }
    
    public Pointbase(double xx, double yy, int ttype){
    	id = 0; x = xx; y = yy; type = ttype;
    }
    
    public Pointbase(int idd, double xx, double yy, int ttype){
    	id = idd; x = xx; y = yy; type = ttype;
    }
    
    //rotate a point by angle theta, not used;
    public void rotate(double theta)
    {
         double cosa = Math.cos(theta), sina = Math.sin(theta),
                newx,newy;
         newx = x*cosa-y*sina;
         newy = x*sina+y*cosa;
         x = newx;
         y = newy;
    }

    //operator overloading
    //friend  bool operator==(const Pointbase&, const Pointbase&);
    public boolean equals(Object o){
    	if (!(o instanceof Pointbase)) return false;
    	return equals((Pointbase)o);
    }
    
    public boolean equals(Pointbase pb){
    	return (this.x == pb.x) && (this.y == pb.y);
    }
    
    //friend  bool operator>(const Pointbase&, const Pointbase&);
    //friend  bool operator<(const Pointbase&, const Pointbase&);    
    public int compareTo(Object o) {
    	// operator>
    	// return( (pa.y > pb.y) || ( (pa.y==pb.y) && (pa.x < pb.x)) );
    	// operator<
    	// return( (pa.y < pb.y) || ( (pa.y==pb.y) && (pa.x > pb.x)) );
    	if (!(o instanceof Pointbase)) return -1;
    	Pointbase pb = (Pointbase)o;
    	if (this.equals(pb)) return  0;    	
    	if (this.y > pb.y)   return  1;
    	else 
    	if (this.y < pb.y)   return -1;
    	else
       	if (this.x < pb.x)   return  1;
       	else 			     return -1;    	
	}
    
    //friend  bool operator!=(const Pointbase&, const Pointbase&);
    //substitute with !equals(pointbase)
    
    //friend  ostream &operator<<(ostream &os, const Pointbase& point);
    public String toString(){
    	return "Pointbase(["+x+", "+y+"], ID = "+id+", "+Poly2TriUtils.typeToString(type)+")";
    }
}
