/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */
package simulator.elevatorcontrol;

/**
 * Runtime monitor for project 7.  Based on SamplePerformanceMonitor.
 * 
 * @author Rajeev Sharma (rdsharma)
 */
public class Proj7RuntimeMonitor extends simulator.framework.RuntimeMonitor {

    AtFloorStateMachine atFloorState = new AtFloorStateMachine();
    DoorReversalStateMachine doorReversalState =
            new DoorReversalStateMachine();
    DoorStateMachine doorState = new DoorStateMachine();
    WeightStateMachine weightState = new WeightStateMachine();
    Stopwatch doorReversalStopwatch = new Stopwatch();
    boolean hasMoved = false;
    boolean wasOverweight = false;
    int overWeightCount = 0;
    int wastedOpeningsCount = 0;

    public Proj7RuntimeMonitor() {
    }

    @Override
    protected String[] summarize() {
        String[] arr = new String[3];
        arr[0] = "Overweight count = " + overWeightCount;
        arr[1] = "Wasted openings count = " + wastedOpeningsCount;
        arr[2] = "Time dealing with door reversals = "
                + doorReversalStopwatch.getAccumulatedTime().toString();
        return arr;
    }

    public void timerExpired(Object callbackData) {
        //do nothing
    }

    /**************************************************************************
     * high level event methods
     *
     * these are called by the logic in the message receiving methods and the
     * state machines
     **************************************************************************/
    /**
     * Called once when the door starts opening
     * @param hallway which door the event pertains to
     */
    private void doorOpening(simulator.framework.Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Opening");

        // Determine if this is a wasted call
        int floor = atFloorState.getFloor();
        // Check for an erroneous door opening and make sure floor is in range
        if (floor == MessageDictionary.NONE) {
            ++wastedOpeningsCount;
            return;
        }
        // Check for car call
        if (carCalls[floor - 1][hallway.ordinal()].pressed())
            return;
        // Check if there was a hall call
        boolean hadCall = false;
        for (simulator.framework.Direction d : simulator.framework.Direction.replicationValues) {
            if (hallCalls[floor - 1][hallway.ordinal()][d.ordinal()].pressed())
                return;
        }
        // No call, by definition this is a wasted opening
        ++wastedOpeningsCount;
    }

    /**
     * Called once when the door starts closing
     * @param hallway which door the event pertains to
     */
    private void doorClosing(simulator.framework.Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Closing");
    }

    /**
     * Called once if the door starts opening after it started closing but before
     * it was fully closed.
     * @param hallway which door the event pertains to
     */
    private void doorReopening(simulator.framework.Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Reopening");
    }

    /**
     * Called once when the doors close completely
     * @param hallway which door the event pertains to
     */
    private void doorClosed(simulator.framework.Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Closed");
        //once all doors are closed, check to see if the car was overweight
        if (!doorState.anyDoorOpen()) {
            if (wasOverweight) {
                message("Overweight");
                overWeightCount++;
                wasOverweight = false;
            }
        }
        // See if this is the end of a door reversal
        if (doorReversalStopwatch.isRunning() == true
                && doorReversalState.hasReversal() == false) {
            doorReversalStopwatch.stop();
        }
    }

    /**
     * Called once when the doors are fully open
     * @param hallway which door the event pertains to
     */
    private void doorOpened(simulator.framework.Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Opened");
    }

    /**
     * Called when the car weight changes
     * @param newWeight an incoming weight sensor value
     */
    private void weightChanged(int newWeight) {
        //System.out.println("Elevator weight changed to " + newWeight);
        if (newWeight > simulator.framework.Elevator.MaxCarCapacity) {
            wasOverweight = true;
        }
    }

    /**
     * Called when a new door reversal occurs
     * @param hallway which door the event pertains to
     * @param side which door the event pertains to
     */
    private void doorReversalStarted(simulator.framework.Hallway hallway, simulator.framework.Side side) {
        // System.out.println(hallway.toString() + " " + side.toString()
        //         + " Door Reversal Started");

        // Start timer if this is the first door reversal since last closing
        if (doorReversalStopwatch.isRunning() == false)
            doorReversalStopwatch.start();
    }

    /**
     * Called when a new door reversal ends
     * @param hallway which door the event pertains to
     * @param side which door the event pertains to
     */
    private void doorReversalEnded(simulator.framework.Hallway hallway, simulator.framework.Side side) {
        // System.out.println(hallway.toString() + " " + side.toString()
        //         + " Door Reversal Ended");
    }

    /**************************************************************************
     * low level message receiving methods
     *
     * These mostly forward messages to the appropriate state machines
     **************************************************************************/
    @Override
    public void receive(simulator.payloads.AtFloorPayload.ReadableAtFloorPayload msg) {
        atFloorState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload msg) {
        doorReversalState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.CarWeightPayload.ReadableCarWeightPayload msg) {
        weightState.receive(msg);
    }

    @Override
    public void receive(simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload msg) {
        if (msg.speed() > 0) {
            hasMoved = true;
        }
    }

    /**
     * Utility class for keeping track of the state of AtFloor.  Provides
     * external methods that can be queried to determine the floor and hallway
     * the elevator is currently at.
     *
     * Currently only updates internal state and does not call any state change
     * handler functions.
     */
    private class AtFloorStateMachine {

        int oldFloor = MessageDictionary.NONE;
        simulator.framework.Hallway oldHallway = simulator.framework.Hallway.NONE;

        public void receive(simulator.payloads.AtFloorPayload.ReadableAtFloorPayload msg) {
            // Update for current floor
            if (msg.getFloor() == oldFloor) {
                // Additional door opening
                if (msg.getValue() == true) {
                    if ( ( oldHallway == simulator.framework.Hallway.FRONT &&
                            msg.getHallway() == simulator.framework.Hallway.BACK )
                            || ( oldHallway == simulator.framework.Hallway.BACK &&
                            msg.getHallway() == simulator.framework.Hallway.FRONT ) ) {
                        oldHallway = simulator.framework.Hallway.BOTH;
                    } else {
                        oldHallway = msg.getHallway();
                    }
                // Leaving floor
                } else {
                    // Simple case
                    if (msg.getHallway() == oldHallway) {
                        oldHallway = simulator.framework.Hallway.NONE;
                        oldFloor = MessageDictionary.NONE;
                    // Handle weirder cases
                    } else if (oldHallway == simulator.framework.Hallway.BOTH) {
                        if (msg.getHallway() == simulator.framework.Hallway.FRONT) {
                            oldHallway = simulator.framework.Hallway.BACK;
                        } else if (msg.getHallway() == simulator.framework.Hallway.BACK) {
                            oldHallway = simulator.framework.Hallway.FRONT;
                        } else if (msg.getHallway() == simulator.framework.Hallway.BOTH) {
                            oldHallway = simulator.framework.Hallway.NONE;
                            oldFloor = MessageDictionary.NONE;
                        }
                    }
                }
            // Arriving at new floor
            } else if (msg.getValue() == true) {
                oldFloor = msg.getFloor();
                oldHallway = msg.getHallway();
            }
        }

        public int getFloor() {
            return oldFloor;
        }

        public simulator.framework.Hallway getHallway() {
            return oldHallway;
        }
    }

    private static enum DoorState {

        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    /**
     * Utility class to detect weight changes
     */
    private class WeightStateMachine {

        int oldWeight = 0;

        public void receive(simulator.payloads.CarWeightPayload.ReadableCarWeightPayload msg) {
            if (oldWeight != msg.weight()) {
                weightChanged(msg.weight());
            }
            oldWeight = msg.weight();
        }
    }

    /**
     * Utility class to detect door reversal changes
     */
    private class DoorReversalStateMachine {

        // Java initializes boolean values to false
        boolean state[][] = new boolean[2][2];

        public void receive(simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload msg) {
            simulator.framework.Hallway hall = msg.getHallway();
            simulator.framework.Side side = msg.getSide();
            int h = hall.ordinal();
            int s = side.ordinal();

            // New reversal
            if (msg.isReversing() == true && state[h][s] == false) {
                state[h][s] = true;
                doorReversalStarted(hall, side);
            // Reversal ending
            } else if (msg.isReversing() == false && state[h][s] == true) {
                state[h][s] = false;
                doorReversalEnded(hall, side);
            }

        }

        public boolean hasReversal() {
            return state[0][0] || state[0][1] || state[1][0] || state[1][1];
        }
    }

    /**
     * Utility class for keeping track of the door state.
     *
     * Also provides external methods that can be queried to determine the
     * current door state.
     */
    private class DoorStateMachine {

        DoorState state[] = new DoorState[2];

        public DoorStateMachine() {
            state[simulator.framework.Hallway.FRONT.ordinal()] = Proj7RuntimeMonitor.DoorState.CLOSED;
            state[simulator.framework.Hallway.BACK.ordinal()] = Proj7RuntimeMonitor.DoorState.CLOSED;
        }

        public void receive(simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload msg) {
            updateState(msg.getHallway());
        }

        public void receive(simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload msg) {
            updateState(msg.getHallway());
        }

        public void receive(simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload msg) {
            updateState(msg.getHallway());
        }

        private void updateState(simulator.framework.Hallway h) {
            DoorState previousState = state[h.ordinal()];

            DoorState newState = previousState;

            if (allDoorsClosed(h) && allDoorMotorsStopped(h)) {
                newState = Proj7RuntimeMonitor.DoorState.CLOSED;
            } else if (allDoorsCompletelyOpen(h) && allDoorMotorsStopped(h)) {
                newState = Proj7RuntimeMonitor.DoorState.OPEN;
                //} else if (anyDoorMotorClosing(h) && anyDoorOpen(h)) {
            } else if (anyDoorMotorClosing(h)) {
                newState = Proj7RuntimeMonitor.DoorState.CLOSING;
            } else if (anyDoorMotorOpening(h)) {
                newState = Proj7RuntimeMonitor.DoorState.OPENING;
            }

            if (newState != previousState) {
                switch (newState) {
                    case CLOSED:
                        doorClosed(h);
                        break;
                    case OPEN:
                        doorOpened(h);
                        break;
                    case OPENING:
                        if (previousState == Proj7RuntimeMonitor.DoorState.CLOSING) {
                            doorReopening(h);
                        } else {
                            doorOpening(h);
                        }
                        break;
                    case CLOSING:
                        doorClosing(h);
                        break;

                }
            }

            //set the newState
            state[h.ordinal()] = newState;
        }

        //door utility methods
        public boolean allDoorsCompletelyOpen(simulator.framework.Hallway h) {
            return doorOpeneds[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].isOpen()
                    && doorOpeneds[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].isOpen();
        }

        public boolean anyDoorOpen() {
            return anyDoorOpen(simulator.framework.Hallway.FRONT) || anyDoorOpen(simulator.framework.Hallway.BACK);

        }

        public boolean anyDoorOpen(simulator.framework.Hallway h) {
            return !doorCloseds[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].isClosed()
                    || !doorCloseds[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].isClosed();
        }

        public boolean allDoorsClosed(simulator.framework.Hallway h) {
            return (doorCloseds[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].isClosed()
                    && doorCloseds[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].isClosed());
        }

        public boolean allDoorMotorsStopped(simulator.framework.Hallway h) {
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.STOP
                    && doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.STOP;
        }

        public boolean anyDoorMotorOpening(simulator.framework.Hallway h) {
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.OPEN
                    || doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.OPEN;
        }

        public boolean anyDoorMotorClosing(simulator.framework.Hallway h) {
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.CLOSE
                    || doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.CLOSE;
        }
    }

    /**
     * Keep track of time and decide whether to or not to include the last interval
     */
    private class ConditionalStopwatch {

        private boolean isRunning = false;
        private jSimPack.SimTime startTime = null;
        private jSimPack.SimTime accumulatedTime = jSimPack.SimTime.ZERO;

        /**
         * Call to start the stopwatch
         */
        public void start() {
            if (!isRunning) {
                startTime = simulator.framework.Harness.getTime();
                isRunning = true;
            }
        }

        /**
         * stop the stopwatch and add the last interval to the accumulated total
         */
        public void commit() {
            if (isRunning) {
                jSimPack.SimTime offset = jSimPack.SimTime.subtract(simulator.framework.Harness.getTime(), startTime);
                accumulatedTime = jSimPack.SimTime.add(accumulatedTime, offset);
                startTime = null;
                isRunning = false;
            }
        }

        /**
         * stop the stopwatch and discard the last interval
         */
        public void reset() {
            if (isRunning) {
                startTime = null;
                isRunning = false;
            }
        }

        public jSimPack.SimTime getAccumulatedTime() {
            return accumulatedTime;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }

    /**
     * Keep track of the accumulated time for an event
     */
    private class Stopwatch {

        private boolean isRunning = false;
        private jSimPack.SimTime startTime = null;
        private jSimPack.SimTime accumulatedTime = jSimPack.SimTime.ZERO;

        /**
         * Start the stopwatch
         */
        public void start() {
            if (!isRunning) {
                startTime = simulator.framework.Harness.getTime();
                isRunning = true;
            }
        }

        /**
         * Stop the stopwatch and add the interval to the accumulated total
         */
        public void stop() {
            if (isRunning) {
                jSimPack.SimTime offset = jSimPack.SimTime.subtract(simulator.framework.Harness.getTime(), startTime);
                accumulatedTime = jSimPack.SimTime.add(accumulatedTime, offset);
                startTime = null;
                isRunning = false;
            }
        }

        public jSimPack.SimTime getAccumulatedTime() {
            return accumulatedTime;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }

    /**
     * Utility class to implement an event detector
     */
    private abstract class EventDetector {

        boolean previousState;

        public EventDetector(boolean initialValue) {
            previousState = initialValue;
        }

        public void updateState(boolean currentState) {
            if (currentState != previousState) {
                previousState = currentState;
                eventOccurred(currentState);
            }
        }

        /**
         * subclasses should overload this to make something happen when the event
         * occurs.
         * @param newState
         */
        public abstract void eventOccurred(boolean newState);
    }
}
