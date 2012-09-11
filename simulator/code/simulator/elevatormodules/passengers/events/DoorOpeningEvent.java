/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers.events;

import simulator.framework.Hallway;

/**
 *
 * @author kamikasee
 */
public class DoorOpeningEvent extends PassengerEvent {
    private final Hallway hallway;

    public DoorOpeningEvent(Hallway hallway) {
        this.hallway = hallway;
    }

    public Hallway getHallway() {
        return hallway;
    }
}
