/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads;

/**
 * Tag interface that prevents controller objects from accessing payloads that are
 * used internally by the simulator.  All payloads that are used by system objects
 * (e.g. DoorPosition, AtFloor sensors, etc) should be tagged with this interface.
 * The correct way for controllers to access this information is through the
 * network messages sent by smart sensors.  The only payload classes that should
 * not be tagged are classes that the controllers in ElevatorControl interface
 * with directly, e.g. CarCallPayload, CarLightPayload, CarLevelPositionPayload, etc.
 *
 * See also PhysicalNetwork.PhysicalConnection, where the runtime check on this class is performed.
 * 
 * @author Justin Ray
 */
public interface InternalPayload {

}
