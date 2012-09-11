/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers;

import simulator.elevatormodules.passengers.events.PassengerEvent;

/**
 * implements a PassengerEvent callback
 *
 * @author Justin Ray
 */
public interface PassengerEventReceiver {
    public void passengerEvent(PassengerEvent e);
}
