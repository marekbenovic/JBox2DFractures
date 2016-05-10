package org.jbox2d.fracture.hertelmehlhorn;

/**
 * Prvok spojoveho zoznamu reprezentujuci vrchol konvexneho polygonu.
 * 
 * @author Marek Benovic
 */
class NodeHM {
    /**
     * Index mnohostenu
     */
    int index;
    

    /**
     * Susedny vrchol mnohostenu
     */
    NodeHM prev, next;
    
    /**
     * Inicializuje uzol vrcholu.
     * @param index Index mnohostenu
     */
    public NodeHM(int index) {
        this.index = index;
    }
}