package org.gui;

import org.jbox2d.dynamics.World;

/**
 * Interface pre testovacie scenare
 *
 * @author Marek Benovic
 */
public interface ICase {
    /**
     * Inicializator. Do sveta predanom v parametri inicializuje objekty.
     * @param w Svet
     */
    public void init(World w);
}
