/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers.events;

import simulator.framework.Direction;

/**
 *
 * @author kamikasee
 */
public class MotionEvent extends PassengerEvent {
    private Direction direction;

    public MotionEvent(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}
