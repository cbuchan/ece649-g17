/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers;

/**
 * Registration interface to allow event recievers to get events from the
 * sender object.
 *
 * @author Justin Ray
 */
public interface PassengerEventSender {
    public void registerReceiver(PassengerEventReceiver receiver);
    public void unregisterReceiver(PassengerEventReceiver receiver);
    public void unregisterReceivers();
}
