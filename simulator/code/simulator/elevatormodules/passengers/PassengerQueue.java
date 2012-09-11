package simulator.elevatormodules.passengers;

import java.util.ArrayList;
import simulator.elevatormodules.passengers.events.PassengerEvent;
import simulator.framework.Direction;
import simulator.framework.Hallway;

/**
 * A queue collection for passengers.  Implemented as a non-generic so that
 * arrays of queues can be used in PassengerHandler.
 * 
 * @author Justin Ray
 */
public class PassengerQueue implements PassengerEventReceiver {

    public final static String EMPTY_TOOLTIP = "<empty>";
    ArrayList<Passenger> passengers = new ArrayList<Passenger>();

    public Passenger peekFirst() {
        if (passengers.isEmpty()) {
            return null;
        }
        return passengers.get(0);
    }

    public Passenger popFirst() {
        if (passengers.isEmpty()) {
            return null;
        }
        return passengers.remove(0);
    }

    public Passenger popLast() {
        if (passengers.isEmpty()) {
            return null;
        }
        return passengers.remove(passengers.size() - 1);
    }

    public Passenger peekLast() {
        if (passengers.isEmpty()) {
            return null;
        }
        return passengers.get(passengers.size() - 1);
    }

    public void remove(Passenger p) {
        passengers.remove(p);
    }

    public void addFirst(Passenger p) {
        passengers.add(0, p);
    }

    public void addLast(Passenger p) {
        passengers.add(p);
    }

    public int getIndex(Passenger p) {
        return passengers.indexOf(p);
    }

    public int size() {
        return passengers.size();
    }

    /**
     * calls mightExit for the given floor and side on each queue item until
     * one returns true
     * @param floor
     * @param side
     * @return first passenger object that wants to exit at the given floor and side
     */
    public Passenger getFirstExitBlocker(int floor, Hallway side) {
        for (Passenger p : passengers) {
            if (p.mightExit(floor, side)) {
                return p;
            }
        }
        return null;
    }

    /**
     * calls mightExit for the given floor, side, and direction on each queue item until
     * one returns true
     * @param floor
     * @param side
     * @param direction
     * @return first passenger object that wants to exit at the given floor and side
     */
    public Passenger getFirstEntryBlocker(int floor, Hallway side, Direction direction) {
        for (Passenger p : passengers) {
            if (p.mightEnter(floor, side, direction)) {
                return p;
            }
        }
        return null;
    }

    public String getToolTipString() {
        if (passengers.size() > 0) {
            StringBuilder b = new StringBuilder("<html>");
            for (Passenger p : passengers) {
                b.append(p.getToolTipLine());
                b.append("<br>");
            }
            b.append("</html>");
            return b.toString();
        } else {
            return EMPTY_TOOLTIP;
        }
    }

    public void passengerEvent(PassengerEvent e) {
        for (Passenger p : passengers) {
            p.passengerEvent(e);
        }
    }
}
