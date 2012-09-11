/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers.events;

/**
 * notify passengers of updated position indicator value
 * @author Justin Ray
 */
public class PositionIndicatorChangedEvent extends PassengerEvent{
    private final int current;
    private final int previous;

    public PositionIndicatorChangedEvent(int current, int previous) {
        this.current = current;
        this.previous = previous;
    }

    public int getCurrentPosition() {
        return current;
    }

    public int getPreviousPosition() {
        return previous;
    }
}
