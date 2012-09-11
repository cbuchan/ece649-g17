/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers.events;

import jSimPack.SimTime;
import simulator.framework.Harness;

/**
 *
 * @author kamikasee
 */
public abstract class PassengerEvent {
    
    private SimTime eventTime;

    public PassengerEvent () {
        this.eventTime = Harness.getTime();
    }

    public PassengerEvent (SimTime eventTime) {
        this.eventTime = eventTime;
    }

    SimTime getEventTime() {
        return eventTime;
    }
}
