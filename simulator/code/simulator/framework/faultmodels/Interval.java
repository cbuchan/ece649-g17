/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework.faultmodels;

import jSimPack.SimTime;

/**
 * Trivial interval implementation that doesn't do anything on events.
 * @author justinr2
 */
public class Interval extends AbstractInterval {

    public Interval (SimTime startTime, SimTime duration) {
        super(startTime, duration);
    }
    
    @Override
    public void startEvent() {
        //do nothing
    }

    @Override
    public void endEvent() {
        //do nothing
    }

}
