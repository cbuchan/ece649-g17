/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework.faults;

import simulator.framework.Logger;

/**
 * Utility superclass for fault objects.
 * 
 * @author Justin Ray
 */
public abstract class Fault extends Logger {

    public Fault() {
        super("Fault");
    }
    public Fault(String name) {
        super(name);
    }
    
    public String getFaultStats() {
        return name + ": no stats";
    }

}
