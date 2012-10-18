/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatorcontrol;

/**
 * This monitoring class gives you a starting point for the performance checks
 * you will do in project 7.
 *
 * This monitor detects the number of stops where the car becomes overweight
 * when the door are open.  Each stop counted at most once, unless the doors
 * close completely and then reopen at the same floor.
 *
 * See the documentation of simulator.framework.RuntimeMonitor for more details.
 * 
 * @author Justin Ray
 */
public class SamplePerformanceMonitor extends simulator.framework.RuntimeMonitor {

    DoorStateMachine doorState = new DoorStateMachine();
    WeightStateMachine weightState = new WeightStateMachine();
    boolean hasMoved = false;
    boolean wasOverweight = false;
    int overWeightCount = 0;

    public SamplePerformanceMonitor() {
    }

    @Override
    protected String[] summarize() {
        String[] arr = new String[0];
        arr[0] = "Overweight Count = " + overWeightCount;
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
     * @param hallway which door the event pertains to
     */
    private void weightChanged(int newWeight) {
        if (newWeight > simulator.framework.Elevator.MaxCarCapacity) {
            wasOverweight = true;
        }
    }

    /**************************************************************************
     * low level message receiving methods
     *
     * These mostly forward messages to the appropriate state machines
     **************************************************************************/
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
     * Utility class for keeping track of the door state.
     *
     * Also provides external methods that can be queried to determine the
     * current door state.
     */
    private class DoorStateMachine {

        DoorState state[] = new DoorState[2];

        public DoorStateMachine() {
            state[simulator.framework.Hallway.FRONT.ordinal()] = SamplePerformanceMonitor.DoorState.CLOSED;
            state[simulator.framework.Hallway.BACK.ordinal()] = SamplePerformanceMonitor.DoorState.CLOSED;
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
                newState = SamplePerformanceMonitor.DoorState.CLOSED;
            } else if (allDoorsCompletelyOpen(h) && allDoorMotorsStopped(h)) {
                newState = SamplePerformanceMonitor.DoorState.OPEN;
                //} else if (anyDoorMotorClosing(h) && anyDoorOpen(h)) {
            } else if (anyDoorMotorClosing(h)) {
                newState = SamplePerformanceMonitor.DoorState.CLOSING;
            } else if (anyDoorMotorOpening(h)) {
                newState = SamplePerformanceMonitor.DoorState.OPENING;
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
                        if (previousState == SamplePerformanceMonitor.DoorState.CLOSING) {
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
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.STOP && doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.STOP;
        }

        public boolean anyDoorMotorOpening(simulator.framework.Hallway h) {
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.OPEN || doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.OPEN;
        }

        public boolean anyDoorMotorClosing(simulator.framework.Hallway h) {
            return doorMotors[h.ordinal()][simulator.framework.Side.LEFT.ordinal()].command() == simulator.framework.DoorCommand.CLOSE || doorMotors[h.ordinal()][simulator.framework.Side.RIGHT.ordinal()].command() == simulator.framework.DoorCommand.CLOSE;
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
