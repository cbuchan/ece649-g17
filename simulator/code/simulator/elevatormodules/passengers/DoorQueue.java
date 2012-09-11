/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules.passengers;

import java.util.ArrayList;
import java.util.Comparator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.ReplicationComputer;

/**
 * This class arbitrates access to a physical doorway.  It maintains a separate
 * queue for passengers that want to enter and exit, priority is given to 
 * passengers exiting the door.
 * 
 * Passengers use the request method to add themselves to the appropriate queue,
 * and then check "isNext" to see if it is their turn or not.
 *
 * @author justinr2
 *
 * Bug Fixes:
 * 20110429 JDR added contains(Passenger) method to support bug fixes in Passenger.
 */
public class DoorQueue {

    ArrayList<Passenger> enterQueue = new ArrayList<Passenger>();
    ArrayList<Passenger> exitQueue = new ArrayList<Passenger>();
    private final String name;
    private final Hallway hallway;

    public DoorQueue(Hallway hallway) {
        this.hallway = hallway;
        this.name = "DoorQueue" + ReplicationComputer.makeReplicationString(hallway);
    }

    public void requestEnter(Passenger p) {
        if (enterQueue.contains(p) || exitQueue.contains(p)) {
            throw new RuntimeException("Tried to add " + p + " to the queue twice.");
        }
        enterQueue.add(p);
    }

    public void requestExit(Passenger p) {
        if (enterQueue.contains(p) || exitQueue.contains(p)) {
            throw new RuntimeException("Tried to add " + p + " to the queue twice.");
        }
        exitQueue.add(p);
    }

    public void remove(Passenger p) {
        if (enterQueue.contains(p)) {
            enterQueue.remove(p);
        }
        if (exitQueue.contains(p)) {
            exitQueue.remove(p);
        }
    }

    public void clear() {
        enterQueue.clear();
        exitQueue.clear();
    }

    public boolean contains(Passenger p) {
        return enterQueue.contains(p) || exitQueue.contains(p);
    }

    public boolean isNext(Passenger p) {
        //log("isNext(",p,")");
        //printDoorQueue();
        if (exitQueue.indexOf(p) == 0) {
            return true;
        } else if (enterQueue.indexOf(p) == 0 && exitQueue.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private void printDoorQueue() {
        log("Door Queue ", this.hallway, ":");
        log("  Exit Queue:");
        if (exitQueue.isEmpty()) {
            log("    <empty>");
        } else {
            for (Passenger p : exitQueue) {
                log("    ", p.getInfoLine());
            }
        }
        log("  Enter Queue:");
        if (enterQueue.isEmpty()) {
            log("    <empty>");
        } else {
            for (Passenger p : enterQueue) {
                log("    ", p.getInfoLine());
            }
        }
    }

    private void log(Object... msg) {
        Harness.log(name, msg);
    }

    private class PassengerCompare implements Comparator<Passenger> {

        public int compare(Passenger o1, Passenger o2) {
            return o1.getInfo().injectionTime.compareTo(o2.getInfo().injectionTime);
        }
    }
}
