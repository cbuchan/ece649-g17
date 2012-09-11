/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules.passengers;

import jSimPack.SimTime;
import java.text.ParseException;
import java.util.Random;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.Hallway;

/**
 * Data class for passenger parameters
 * The class is constructed with the deterministic passenger parameters and
 * also builds a set of random parameters based on a random seed.
 *
 * All the data values are public final, and they are all immutable so the
 * class is effectively immutable once constructed.  Any additions to this class
 * should follow this pattern, or use a get wrapper for mutable objects.
 *
 * @author Justin Ray
 */
public final class PassengerInfo {
    //constructed parameters

    public final int startFloor;
    public final Hallway startHallway;
    public final int endFloor;
    public final Hallway endHallway;
    public final Direction travelDirection; /** The direction the passenger will travel in.  see computeTravelDirection */
    public final Direction hallCallDirection; /** The hall call the passenger presses.  see computeHallCallDirection */
    public final SimTime injectionTime;
    //random parameters
    public final SimTime hallPressTime;
    public final SimTime carPressTime;
    public final int width;
    public final SimTime doorTraversalDelay;
    public final SimTime doorBackoutDelay;
    public final int missedOpeningThreshold;
    private final Random random;

    /**
     * Typed constructor
     * @param injectionTime
     * @param startFloor
     * @param startHallway
     * @param endFloor
     * @param endHallway
     * @param randomSeed
     */
    public PassengerInfo(SimTime injectionTime, int startFloor, Hallway startHallway, int endFloor, Hallway endHallway, long randomSeed) {
        this.injectionTime = injectionTime;
        this.startFloor = startFloor;
        this.startHallway = startHallway;
        this.endFloor = endFloor;
        this.endHallway = endHallway;

        try {
            validate();
        } catch (PassengerException ex) {
            throw new RuntimeException(ex);
        }

        //construct random parameters
        random = new Random(randomSeed);
        travelDirection = computeTravelDirection(startFloor, endFloor);
        hallCallDirection = computeHallCallDirection(startFloor, endFloor, random);
        hallPressTime = getRandomTime(250, 1000);
        carPressTime = getRandomTime(250, 1000);
        width = getRandom(20, 45);
        doorTraversalDelay = getRandomTime(1000, 2000);
        doorBackoutDelay = getRandomTime(2000, 4000);
        missedOpeningThreshold = getRandom(3, 5);


    }

    /**
     * Parsing constructor, format is
     * INJECTIONTIME STARTFLOOR STARTHALL ENDFLOOR ENDHALL
     * @param words
     * @throws ParseException
     */
    public PassengerInfo(String[] words, long randomSeed, SimTime prevTime) throws ParseException {
        int index = 0;
        SimTime tempTime = SimTime.ZERO;
        if (words.length != 5) {
            throw new ParseException("Expected 5 items for passenger:  injectionTime, startFloor, startHall, endFloor, endHall", 0);
        }
        try {
            tempTime = new SimTime(words[index]);
        	//check for incremental time value, if it is, add the increment to the prev, otherwise just take its value 
        	if (words[index++].startsWith("+")){
        		injectionTime = SimTime.add(tempTime, prevTime);
        	}
        	else{
        		injectionTime = tempTime;
        	}
            startFloor = Integer.parseInt(words[index++]);
            startHallway = Hallway.valueOf(words[index++]);
            endFloor = Integer.parseInt(words[index++]);
            endHallway = Hallway.valueOf(words[index++]);
        } catch (NumberFormatException ex) {
            throw new ParseException(ex.getMessage(), index + 2);
        } catch (IllegalArgumentException ex) {
            throw new ParseException(ex.getMessage(), index + 1);
        }
        try {
            validate();
        } catch (PassengerException ex) {
            throw new ParseException(ex.getMessage(), -1);
        }
        //construct random parameters
        random = new Random(randomSeed);
        travelDirection = computeTravelDirection(startFloor, endFloor);
        hallCallDirection = computeHallCallDirection(startFloor, endFloor, random);
        hallPressTime = getRandomTime(250, 1000);
        carPressTime = getRandomTime(250, 1000);
        width = getRandom(20, 45);
        doorTraversalDelay = getRandomTime(1000, 2000);
        doorBackoutDelay = getRandomTime(2000, 4000);
        missedOpeningThreshold = getRandom(3, 5);
    }

    private void validate() throws PassengerException {
        //check
        if (injectionTime.isNegative()) {
            throw new PassengerException("Passenger injection time " + injectionTime + " cannot be negative.");
        }
        //check start
        //allow zero value for start, because this means the passenger starts in the car
        if (startFloor < 0 || startFloor > Elevator.numFloors) {
            throw new PassengerException("Start floor " + startFloor + " out of range.");
        }
        if (startFloor == 0 && !injectionTime.equals(SimTime.ZERO)) {
            throw new PassengerException("Start floor " + startFloor + " is only allowed to be injected at time 0.");
        }
        if (startHallway != Hallway.FRONT && startHallway != Hallway.BACK) {
            throw new PassengerException("Start hallway " + startHallway + " out of range.");
        }
        if (startFloor != 0) {
            if (!Elevator.hasLanding(startFloor, startHallway)) {
                throw new PassengerException("No landing at " + startFloor + "," + startHallway + ".");
            }
        }
        //check end
        if (endFloor < 1 || endFloor > Elevator.numFloors) {
            throw new PassengerException("End floor " + endFloor + " out of range.");
        }
        if (endHallway != Hallway.FRONT && endHallway != Hallway.BACK) {
            throw new PassengerException("End hallway " + endHallway + " out of range.");
        }
        if (!Elevator.hasLanding(endFloor, endHallway)) {
            throw new PassengerException("No landing at " + endFloor + "," + endHallway + ".");
        }


    }


    private static Direction computeTravelDirection(int startFloor, int endFloor) {
        if (startFloor == 0) return Direction.STOP; //start in the car, so no direction

        if (startFloor > endFloor) {
            return Direction.DOWN;
        }
        if (startFloor < endFloor) {
            return Direction.UP;
        }
        //if we get to here, then they are equal, so return a random direction.
        return Direction.STOP;
    }

    private static Direction computeHallCallDirection(int startFloor, int endFloor, Random random) {
        if (startFloor == 0) return Direction.STOP; //start in the car, so no direction
        if (startFloor > endFloor) {
            return Direction.DOWN;
        }
        if (startFloor < endFloor) {
            return Direction.UP;
        }
        //if we get to here, then the floors are equal, so return a random direction.
        //except if we are at the top or bottom, because there are not two buttons
        //there.
        if (startFloor == 1) return Direction.UP;
        if (startFloor == Elevator.numFloors) return Direction.DOWN;
        if (random.nextBoolean()) {
            return Direction.DOWN;
        } else {
            return Direction.UP;
        }
    }

    private int getRandom(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private SimTime getRandomTime(int minMs, int maxMs) {
        return new SimTime(getRandom(minMs, maxMs), SimTime.SimTimeUnit.MILLISECOND);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(injectionTime);
        sb.append(" ");
        sb.append(startFloor);
        sb.append(" ");
        sb.append(startHallway);
        sb.append(" ");
        sb.append(endFloor);
        sb.append(" ");
        sb.append(endHallway);
        sb.append(" ");
        return sb.toString();
    }
}
