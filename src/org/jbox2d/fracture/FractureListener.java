package org.jbox2d.fracture;

import java.util.List;
import org.jbox2d.dynamics.Body;

/**
 * Listener sluziaci na odchytavanie triestenia telies.
 *
 * @author Marek Benovic
 */
@FunctionalInterface
public interface FractureListener {
    /**
     * Handler spustajuci sa po roztriesteni objektu.
     * @param material kontakt, ktory vyvolal rozpad
     * @param intensity intenzita rozpadu
     * @param fragments polygony fragmentacie
     */
    public void action(Material material, float intensity, List<Body> fragments);
}