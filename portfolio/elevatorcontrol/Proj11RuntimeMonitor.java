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

import simulator.elevatormodules.*;
import simulator.framework.*;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.CarWeightPayload;
import simulator.payloads.DoorClosedPayload;
import simulator.payloads.DoorMotorPayload;
import simulator.payloads.DoorOpenPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;

/**
 * @author Jesse Salazar, Jessica Tiu
 */
public class Proj11RuntimeMonitor extends RuntimeMonitor {

    // checks RT6, RT9; updates RT8.3
    DriveStateMachine driveState = new DriveStateMachine();
    // checks RT10; updates RT7, RT8.1, RT8.2
    DoorStateMachine doorState = new DoorStateMachine();
    RT7StateMachine rt7State = new RT7StateMachine();
    RT81StateMachine rt81State = new RT81StateMachine();
    RT82StateMachine rt82State = new RT82StateMachine();
    RT83StateMachine rt83State = new RT83StateMachine();

    boolean hasMoved = false;
    boolean wasOverweight = false;
    boolean hasReversal = false;
    int overWeightCount = 0;
    int undesiredStopCount = 0;         // elevator stops at a floor with no calls
    int undesiredOpeningsCount = 0;     // elevator opens doors at floor with no calls
    int carLanternOnCount = 0;          // car lantern does not turn on when it should
    int directionChangeCount = 0;       // lantern changes while doors are open
    int notLanternCount = 0;            // elevator services calls in direction other than lantern
    int notFastCount = 0;               // drive was not commanded to fast when possible
    int nudgeBeforeReversalCount = 0;   // count for nudges happening before a door reversal

    protected int currentFloor = MessageDictionary.NONE;
    protected int lastStoppedFloor = MessageDictionary.NONE;
    protected boolean lastLantern[] = {false,false};    // state of lantern(d) last time drive stopped
    protected boolean hadCalls[] = {true,true};         // if serviced floor(h) had calls
    protected boolean fastSpeedReached = false;

    public Proj11RuntimeMonitor() {
        //initialization goes here
    }

    @Override
    protected String[] summarize() {
        String[] arr = new String[7];
        arr[0] = "R-T6: Stopped at floor with no calls = " + undesiredStopCount;
        arr[1] = "R-T7: Doors opened at floor with no calls = " + undesiredOpeningsCount;
        arr[2] = "R-T8.1: Car lantern not lighted for call on another floor = " + carLanternOnCount;
        arr[3] = "R-T8.2: Car lantern changed direction while doors open = " + directionChangeCount;
        arr[4] = "R-T8.3: Elevator serviced call in direction other than lantern = " + notLanternCount;
        arr[5] = "R-T9: Elevator not commanded to fast speed = " + notFastCount;
        arr[6] = "R-T10: Door nudged before reversal = " + nudgeBeforeReversalCount;

        return arr;
    }

    public void timerExpired(Object callbackData) {
        //implement time-sensitive behaviors here
    }

    @Override
    public void receive(ReadableAtFloorPayload msg) {
        updateCurrentFloor(msg);
    }

    @Override
    public void receive(ReadableDoorMotorPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(DoorClosedPayload.ReadableDoorClosedPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(DoorOpenPayload.ReadableDoorOpenPayload msg) {
        doorState.receive(msg);
    }

    @Override
    public void receive(ReadableDriveSpeedPayload msg) {
        driveState.receive(msg);
        checkFastSpeed(msg);
        if (msg.speed() > 0) {
            hasMoved = true;
        }
    }

    /**************************************************************************
     * high level event methods
     *
     * these are called by the logic in the message receiving methods and the
     * state machines
     **************************************************************************/
    /**
     * Called once when the door starts opening
     *
     * @param hallway which door the event pertains to
     */
    private void doorOpening(Hallway hallway) {
    }

    /**
     * Called once when the door starts closing
     *
     * @param hallway which door the event pertains to
     */
    private void doorClosing(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Closing");
    }

    /**
     * Called once if the door starts opening after it started closing but before
     * it was fully closed.
     *
     * @param hallway which door the event pertains to
     */
    private void doorReopening(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Reopening");
        hasReversal = true;
    }

    /**
     * Called once when the doors close completely
     *
     * @param hallway which door the event pertains to
     */
    private void doorClosed(Hallway hallway) {
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
     *
     * @param hallway which door the event pertains to
     */
    private void doorOpened(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Opened");
    }

    /**
     * Called when the car weight changes
     */
    private void weightChanged(int newWeight) {
        if (newWeight > Elevator.MaxCarCapacity) {
            wasOverweight = true;
        }
    }

    /**
     * Warn if the drive was never commanded to fast when fast speed could be
     * used.
     *
     * @param msg
     */
    private void checkFastSpeed(ReadableDriveSpeedPayload msg) {
        if (msg.speed() == 0 && currentFloor != MessageDictionary.NONE) {
            //stopped at a floor
            if (lastStoppedFloor != currentFloor) {
                //we've stopped at a new floor
                if (fastSpeedAttainable(lastStoppedFloor, currentFloor)) {
                    //check and see if the drive was ever reached fast
                    if (!fastSpeedReached) {
                        warning("The drive was not commanded to FAST on the trip between " +
                                lastStoppedFloor + " and " + currentFloor);
                        notFastCount++;

                    }
                }
                //now that the check is done, set the lastStoppedFloor to this floor
                lastStoppedFloor = currentFloor;
                //reset fastSpeedReached
                fastSpeedReached = false;
            }
        }
        if (msg.speed() > DriveObject.SlowSpeed) {
            //if the drive exceeds the Slow Speed, the drive must have been commanded to fast speed.
            fastSpeedReached = true;
        }
    }

    /*--------------------------------------------------------------------------
     * State Machines
     *------------------------------------------------------------------------*/

    private static enum DriveState {
        STOPPED,
        MOVING,
        STOPPED_UNDESIRED
    }

    /**
     * Utility class for testing the high level requirement:
     * R-T6: The Car shall only stop at Floors for which there are pending calls.
     */
    private class DriveStateMachine {

        DriveState driveState;

        public DriveStateMachine() {
            driveState = DriveState.STOPPED;
        }

        public void receive(ReadableDriveSpeedPayload msg) {
            checkFastSpeed(msg);    // check R-T9
            updateState(msg.speed(), msg.direction());    // update T-R6
        }

        private void updateState(double spd, Direction d) {
            DriveState prevState = driveState;
            DriveState newState = prevState;

            switch (prevState) {
                case STOPPED:
                    // T-R6.1
                    if (spd != DriveObject.StopSpeed) {
                        newState = DriveState.MOVING;
                    }
                    break;
                case MOVING:
                    // T-R6.2
                    if (spd == DriveObject.StopSpeed && (mDesiredFloor.getFloor() == currentFloor) ||
                            !(hasCall(1, Direction.UP) || hasCall(Elevator.numFloors, Direction.DOWN))) {
                        newState = DriveState.STOPPED;
                    }
                    // T-R6.3
                    else if (spd == DriveObject.StopSpeed && mDesiredFloor.getFloor() != currentFloor) {
                        newState = DriveState.STOPPED_UNDESIRED;
                    }
                    break;
                case STOPPED_UNDESIRED:
                    // T-R6.4
                    if (spd != DriveObject.StopSpeed) {
                        newState = DriveState.MOVING;
                    }
                    break;
                default:
                    break;
            }

            if (newState != prevState) {
                switch (newState) {
                    case STOPPED:
                        lastLantern[Direction.UP.ordinal()] = carLanterns[Direction.UP.ordinal()].lighted();
                        lastLantern[Direction.DOWN.ordinal()] = carLanterns[Direction.DOWN.ordinal()].lighted();
                        break;
                    case MOVING:
                        rt7State.updateCalls();
                        rt83State.updateState(d);    // update T-R8.3
                        break;
                    case STOPPED_UNDESIRED:
                        warning("Stopped at floor with no calls");
                        undesiredStopCount++;
                        lastLantern[Direction.UP.ordinal()] = carLanterns[Direction.UP.ordinal()].lighted();
                        lastLantern[Direction.DOWN.ordinal()] = carLanterns[Direction.DOWN.ordinal()].lighted();
                        break;
                    default:
                        break;
                }
            }
            driveState = newState;

        }

        // return true if there is any call in direction d from floor f
        public boolean hasCall(int f, Direction d) {
            if (d == Direction.UP) {
                for (int i = f; i <= Elevator.numFloors; i++) {
                    if (carCalls[f - 1][Hallway.FRONT.ordinal()].isPressed() ||
                            carCalls[f - 1][Hallway.BACK.ordinal()].isPressed() ||
                            hallCalls[f - 1][Hallway.FRONT.ordinal()][d.ordinal()].pressed() ||
                            hallCalls[f - 1][Hallway.BACK.ordinal()][d.ordinal()].pressed())
                        return true;
                }
            } else if (d == Direction.DOWN) {
                for (int i = f; i >= 1; i--) {
                    if (carCalls[f - 1][Hallway.FRONT.ordinal()].isPressed() ||
                            carCalls[f - 1][Hallway.BACK.ordinal()].isPressed() ||
                            hallCalls[f - 1][Hallway.FRONT.ordinal()][d.ordinal()].pressed() ||
                            hallCalls[f - 1][Hallway.BACK.ordinal()][d.ordinal()].pressed())
                        return true;
                }
            }
            return false;
        }
    }


    private static enum RT7State {

        DOOR_OPEN,
        DOOR_OPEN_DESIRED,
        DOOR_OPEN_UNDESIRED
    }

    /**
     * Utility class for testing the high level requirement:
     * R-T7: The Car shall only open Doors at Hallways for which there are pending calls.
     */
    private class RT7StateMachine {

        RT7State rt7State;

        public RT7StateMachine() {
            rt7State = rt7State.DOOR_OPEN;
        }

        // called before drive stops
        private void updateCalls() {
            hadCalls[Hallway.BACK.ordinal()] =
                    carCalls[mDesiredFloor.getFloor()-1][Hallway.BACK.ordinal()].isPressed() ||
                    hallCalls[mDesiredFloor.getFloor()-1][Hallway.BACK.ordinal()][Direction.UP.ordinal()].pressed() ||
                    hallCalls[mDesiredFloor.getFloor()-1][Hallway.BACK.ordinal()][Direction.DOWN.ordinal()].pressed();
            hadCalls[Hallway.FRONT.ordinal()] =
                    carCalls[mDesiredFloor.getFloor()-1][Hallway.FRONT.ordinal()].isPressed() ||
                    hallCalls[mDesiredFloor.getFloor()-1][Hallway.FRONT.ordinal()][Direction.UP.ordinal()].pressed() ||
                    hallCalls[mDesiredFloor.getFloor()-1][Hallway.FRONT.ordinal()][Direction.DOWN.ordinal()].pressed();
        }

        // this method gets called every time doorState enters the OPEN state
        private void updateState(Hallway hallway) {
            RT7State prevState = rt7State;
            RT7State newState = prevState;

            switch (newState) {

                case DOOR_OPEN:
                    // T-R7.1
                    if (mDesiredFloor.getFloor()==currentFloor && hadCalls[hallway.ordinal()]) {
                        newState = RT7State.DOOR_OPEN_DESIRED;
                    }
                    // T-R7.3
                    else if (mDesiredFloor.getFloor()==currentFloor && !hadCalls[hallway.ordinal()]) {
                        newState = RT7State.DOOR_OPEN_UNDESIRED;
                    }
                    break;

                case DOOR_OPEN_DESIRED:
                    // T-R7.2
                    if (doorState.anyDoorOpen(hallway)) {
                        newState = RT7State.DOOR_OPEN;
                    }
                    break;

                case DOOR_OPEN_UNDESIRED:
                    // T-R7.4
                    if (doorState.anyDoorOpen(hallway)) {
                        newState = RT7State.DOOR_OPEN;
                    }
                    break;

                default:
                    break;
            }

            if (newState != prevState) {
                switch (newState) {
                    case DOOR_OPEN:
                        break;
                    case DOOR_OPEN_DESIRED:
                        hadCalls[hallway.ordinal()]=false;   // reset
                        break;
                    case DOOR_OPEN_UNDESIRED:
                        message("Doors opened at floor with no calls");
                        undesiredOpeningsCount++;
                        hadCalls[hallway.ordinal()]=false;   // reset
                        break;
                    default:
                        break;
                }
            }

            rt7State = newState;

        }


    }


    private static enum RT81State {
        CAR_LANTERN_OFF,
        CAR_LANTERN_ON_PENDING,
        CAR_LANTERN_OFF_INVALID
    }

    /**
     * Utility class for testing the high level requirement:
     * R-T8.1: If any door is open at a hallway and there are any pending calls at any other floor(s),
     * a Car Lantern shall turn on.
     */
    private class RT81StateMachine {

        RT81State rt81State;

        public RT81StateMachine() {
            rt81State = RT81State.CAR_LANTERN_OFF;
        }

        // this method gets called every time doorState enters the OPEN state.
        private void updateState() {
            RT81State prevState = rt81State;
            RT81State newState = prevState;

            switch (prevState) {
                case CAR_LANTERN_OFF:
                    // T-R8.1.1
                    if ((carLanterns[Direction.UP.ordinal()].lighted() &&
                            mDesiredFloor.getDirection() == Direction.UP) ||
                            (carLanterns[Direction.DOWN.ordinal()].lighted() &&
                                    mDesiredFloor.getDirection() == Direction.DOWN) &&
                                    (atFloors[mDesiredFloor.getFloor() - 1][Hallway.BACK.ordinal()].getValue() ||
                                            atFloors[mDesiredFloor.getFloor() - 1][Hallway.FRONT.ordinal()].getValue()) &&
                                    (doorState.allDoorsClosed(Hallway.BACK) &&
                                            doorState.allDoorsClosed(Hallway.FRONT))) {
                        newState = RT81State.CAR_LANTERN_ON_PENDING;
                    }
                    // T-R8.1.3
                    else if ((!carLanterns[Direction.UP.ordinal()].lighted() &&
                            mDesiredFloor.getDirection() == Direction.UP) ||
                            (!carLanterns[Direction.DOWN.ordinal()].lighted() &&
                                    mDesiredFloor.getDirection() == Direction.DOWN) &&
                                    (atFloors[mDesiredFloor.getFloor() - 1][Hallway.BACK.ordinal()].getValue() ||
                                            atFloors[mDesiredFloor.getFloor() - 1][Hallway.FRONT.ordinal()].getValue()) &&
                                    (doorState.allDoorsClosed(Hallway.BACK) &&
                                            doorState.allDoorsClosed(Hallway.FRONT))) {
                        newState = RT81State.CAR_LANTERN_OFF_INVALID;
                    }
                    break;

                case CAR_LANTERN_OFF_INVALID:
                    // T-R8.1.4
                    if ((carLanterns[Direction.UP.ordinal()].lighted() &&
                            mDesiredFloor.getDirection() == Direction.UP) ||
                            (carLanterns[Direction.DOWN.ordinal()].lighted() &&
                                    mDesiredFloor.getDirection() == Direction.DOWN) ||
                            !doorState.anyDoorOpen()) {
                        newState = RT81State.CAR_LANTERN_OFF;
                    }
                    break;

                case CAR_LANTERN_ON_PENDING:
                    // T-R8.1.2
                    if ((carLanterns[Direction.UP.ordinal()].lighted() &&
                            mDesiredFloor.getDirection() == Direction.UP) ||
                            (carLanterns[Direction.DOWN.ordinal()].lighted() &&
                                    mDesiredFloor.getDirection() == Direction.DOWN) ||
                            !doorState.anyDoorOpen()) {
                        newState = RT81State.CAR_LANTERN_OFF;
                    }
                    break;
                default:
                    break;
            }

            if (newState != prevState) {
                switch (newState) {
                    case CAR_LANTERN_OFF:
                        break;
                    case CAR_LANTERN_ON_PENDING:
                        break;
                    case CAR_LANTERN_OFF_INVALID:
                        warning("Car lantern not lighted for call on another floor");
                        carLanternOnCount++;
                        break;
                    default:
                        break;
                }
            }
            rt81State = newState;

        }

    }


    private static enum RT82State {
        CAR_LANTERN_OFF,
        CAR_LANTERN_ON_PENDING,
        CAR_LANTERN_OFF_INVALID
    }

    /**
     * Utility class for testing the high level requirement:
     * R-T8.2: If one of the car lanterns is lit, the direction indicated shall not change while the doors are open.
     */
    private class RT82StateMachine {

        RT82State rt82State[] = new RT82State[2];

        public RT82StateMachine() {
            rt82State[Direction.UP.ordinal()] = RT82State.CAR_LANTERN_OFF;
            rt82State[Direction.DOWN.ordinal()] = RT82State.CAR_LANTERN_OFF;
        }

        // this method gets called every time doorState enters the OPEN state.
        private void updateState(Direction d) {
            RT82State prevState = rt82State[d.ordinal()];
            RT82State newState = prevState;

            switch (prevState) {
                case CAR_LANTERN_OFF:
                    // T-R8.2.1
                    if (carLanterns[d.ordinal()].lighted() &&
                            doorState.anyDoorOpen()) {
                        newState = RT82State.CAR_LANTERN_ON_PENDING;
                    }
                    break;

                case CAR_LANTERN_OFF_INVALID:
                    // T-R8.2.4
                    if (!doorState.anyDoorOpen()) {
                        newState = RT82State.CAR_LANTERN_OFF;
                    }
                    break;

                case CAR_LANTERN_ON_PENDING:
                    // T-R8.2.2
                    if (!doorState.anyDoorOpen()) {
                        newState = RT82State.CAR_LANTERN_OFF;
                    }

                    // T-R8.2.3
                    else if (!carLanterns[d.ordinal()].lighted() ||
                            carLanterns[otherDirection(d).ordinal()].lighted()) {
                        newState = RT82State.CAR_LANTERN_OFF_INVALID;
                    }
                    break;

                default:
                    break;
            }

            if (newState != prevState) {
                switch (newState) {
                    case CAR_LANTERN_OFF:
                        break;
                    case CAR_LANTERN_ON_PENDING:
                        break;
                    case CAR_LANTERN_OFF_INVALID:
                        warning("Car lantern changed from " + d + " while doors open");
                        directionChangeCount++;
                        break;
                    default:
                        break;
                }
            }
            rt82State[d.ordinal()] = newState;
        }

        // return the opposite direction: UP->DOWN, DOWN->UP
        public Direction otherDirection(Direction d) {
            return d == Direction.UP ? Direction.DOWN : Direction.UP;
        }

    }


    private static enum RT83State {
        SERVICE_CALL,
        COMPUTE_NEXT_VALID,
        COMPUTE_NEXT_INVALID
    }

    /**
     * Utility class for testing the high level requirement:
     * R-T8.3: If one of the car lanterns is lit, the car shall service any calls in that direction first.
     */
    private class RT83StateMachine {

        RT83State rt83State[] = new RT83State[2];

        public RT83StateMachine() {
            rt83State[Direction.UP.ordinal()] = RT83State.SERVICE_CALL;
            rt83State[Direction.DOWN.ordinal()] = RT83State.SERVICE_CALL;
        }

        // called whenever the drive is moving
        private void updateState(Direction d) {
            RT83State prevState = rt83State[d.ordinal()];
            RT83State newState = prevState;

            switch (prevState) {
                case SERVICE_CALL:
                    // T-R8.3.1
                    if (lastLantern[d.ordinal()] && !lastLantern[rt82State.otherDirection(d).ordinal()] &&
                            driveState.hasCall(lastStoppedFloor, d)) {
                        newState = RT83State.COMPUTE_NEXT_VALID;
                    }

                    // T-R8.3.3
                    else if (!lastLantern[d.ordinal()] && lastLantern[rt82State.otherDirection(d).ordinal()] &&
                            driveState.hasCall(lastStoppedFloor, d)) {
                        newState = RT83State.COMPUTE_NEXT_INVALID;
                    }
                    break;

                case COMPUTE_NEXT_VALID:
                    // T-R8.3.2
                    if (currentFloor == mDesiredFloor.getFloor()) {
                        newState = RT83State.SERVICE_CALL;
                    }

                    // T-R8.3.5
                    else if (!lastLantern[d.ordinal()] && lastLantern[rt82State.otherDirection(d).ordinal()] &&
                            driveState.hasCall(lastStoppedFloor, d)) {
                        newState = RT83State.COMPUTE_NEXT_INVALID;
                    }
                    break;

                case COMPUTE_NEXT_INVALID:
                    // T-R8.3.4
                    if (currentFloor == mDesiredFloor.getFloor()) {
                        newState = RT83State.SERVICE_CALL;
                    }

                    break;

                default:
                    break;
            }

            if (newState != prevState) {
                switch (newState) {
                    case SERVICE_CALL:
                        break;
                    case COMPUTE_NEXT_VALID:
                        break;
                    case COMPUTE_NEXT_INVALID:
                        warning("Elevator serviced call in direction other than lantern");
                        notLanternCount++;
                        break;
                    default:
                        break;
                }
            }
            rt83State[d.ordinal()] = newState;

        }

    }


    /**
     * Utility class to detect weight changes
     */
    private class WeightStateMachine {

        int oldWeight = 0;

        public void receive(CarWeightPayload.ReadableCarWeightPayload msg) {
            if (oldWeight != msg.weight()) {
                weightChanged(msg.weight());
            }
            oldWeight = msg.weight();
        }
    }


    private static enum DoorState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    /**
     * Utility class for keeping track of the door state.
     * <p/>
     * Also provides external methods that can be queried to determine the
     * current door state.
     */
    private class DoorStateMachine {

        DoorState state[] = new DoorState[2];

        public DoorStateMachine() {
            state[Hallway.FRONT.ordinal()] = DoorState.CLOSED;
            state[Hallway.BACK.ordinal()] = DoorState.CLOSED;
        }

        public void receive(DoorClosedPayload.ReadableDoorClosedPayload msg) {
            updateState(msg.getHallway());
        }

        public void receive(DoorOpenPayload.ReadableDoorOpenPayload msg) {
            updateState(msg.getHallway());
        }

        public void receive(DoorMotorPayload.ReadableDoorMotorPayload msg) {
            updateState(msg.getHallway());
        }

        private void updateState(Hallway h) {
            DoorState previousState = state[h.ordinal()];

            DoorState newState = previousState;

            if (allDoorsClosed(h) && allDoorMotorsStopped(h)) {
                newState = DoorState.CLOSED;
            } else if (allDoorsCompletelyOpen(h) && allDoorMotorsStopped(h)) {
                newState = DoorState.OPEN;
                //} else if (anyDoorMotorClosing(h) && anyDoorOpen(h)) {
            } else if (anyDoorMotorClosing(h)) {
                newState = DoorState.CLOSING;
            } else if (anyDoorMotorOpening(h)) {
                newState = DoorState.OPENING;
            }

            if (newState != previousState) {
                switch (newState) {
                    case CLOSED:
                        doorClosed(h);
                        rt82State.updateState(Direction.UP);        // check req T-R8.2
                        rt82State.updateState(Direction.DOWN);
                        rt7State.updateState(h);    // check req T-R7
                        hasReversal = false;                          // reset any reversals
                        break;
                    case OPEN:
                        doorOpened(h);
                        rt81State.updateState();                    // check req T-R8.1
                        rt82State.updateState(Direction.UP);        // check req T-R8.2
                        rt82State.updateState(Direction.DOWN);
                        hasReversal = false;                          // reset any reversals
                        break;
                    case OPENING:
                        if (previousState == DoorState.CLOSING) {
                            doorReopening(h);       // sets hasReversal to true
                        } else {
                            doorOpening(h);
                        }
                        break;
                    case CLOSING:
                        doorClosing(h);

                        // check R-T10
                        if (doorMotors[h.ordinal()][Side.LEFT.ordinal()].command().equals(DoorCommand.NUDGE) ||
                                doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command().equals(DoorCommand.NUDGE))
                            if (!hasReversal) {
                                warning("Door was commanded to nudge before a reversal");
                                nudgeBeforeReversalCount++;
                            }
                        break;

                }
            }

            //set the newState
            state[h.ordinal()] = newState;
        }

        //door utility methods
        public boolean allDoorsCompletelyOpen(Hallway h) {
            return doorOpeneds[h.ordinal()][Side.LEFT.ordinal()].isOpen()
                    && doorOpeneds[h.ordinal()][Side.RIGHT.ordinal()].isOpen();
        }

        public boolean anyDoorOpen() {
            return anyDoorOpen(Hallway.FRONT) || anyDoorOpen(Hallway.BACK);

        }

        public boolean anyDoorOpen(Hallway h) {
            return !doorCloseds[h.ordinal()][Side.LEFT.ordinal()].isClosed()
                    || !doorCloseds[h.ordinal()][Side.RIGHT.ordinal()].isClosed();
        }

        public boolean anyDoorClosed() {
            return anyDoorClosed(Hallway.FRONT) || anyDoorClosed(Hallway.BACK);

        }

        public boolean anyDoorClosed(Hallway h) {
            return !doorOpeneds[h.ordinal()][Side.LEFT.ordinal()].isOpen()
                    || !doorOpeneds[h.ordinal()][Side.RIGHT.ordinal()].isOpen();
        }

        public boolean allDoorsClosed(Hallway h) {
            return (doorCloseds[h.ordinal()][Side.LEFT.ordinal()].isClosed()
                    && doorCloseds[h.ordinal()][Side.RIGHT.ordinal()].isClosed());
        }

        public boolean allDoorMotorsStopped(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.STOP &&
                    doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.STOP;
        }

        public boolean anyDoorMotorOpening(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.OPEN ||
                    doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.OPEN;
        }

        public boolean anyDoorMotorClosing(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.CLOSE ||
                    doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.CLOSE;
        }
    }

    /*--------------------------------------------------------------------------
     * Utility and helper functions
     *------------------------------------------------------------------------*/

    /**
     * Computes whether fast speed is attainable.  In general, it is attainable
     * between any two floors.
     *
     * @param startFloor
     * @param endFloor
     * @return true if Fast speed can be commanded between the given floors, otherwise false
     */
    private boolean fastSpeedAttainable(int startFloor, int endFloor) {
        //fast speed is attainable between all floors
        if (startFloor == MessageDictionary.NONE || endFloor == MessageDictionary.NONE) {
            return false;
        }
        if (startFloor != endFloor) {
            return true;
        }
        return false;
    }


    private void updateCurrentFloor(ReadableAtFloorPayload lastAtFloor) {
        if (lastAtFloor.getFloor() == currentFloor) {
            //the atFloor message is for the currentfloor, so check both sides to see if they a
            if (!atFloors[lastAtFloor.getFloor() - 1][Hallway.BACK.ordinal()].value() &&
                    !atFloors[lastAtFloor.getFloor() - 1][Hallway.FRONT.ordinal()].value()) {
                //both sides are false, so set to NONE
                currentFloor = MessageDictionary.NONE;
            }
            //otherwise at least one side is true, so leave the current floor as is
        } else {
            if (lastAtFloor.value()) {
                currentFloor = lastAtFloor.getFloor();
            }
        }
    }


}
