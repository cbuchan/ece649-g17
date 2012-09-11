/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules.passengers;

import simulator.elevatormodules.passengers.events.PassengerEvent;
import jSimPack.SimTime;
import simulator.elevatormodules.CarButtonLight;
import simulator.elevatormodules.Door;
import simulator.elevatormodules.HallButtonLight;
import simulator.elevatormodules.passengers.DriveMonitor.DriveState;
import simulator.elevatormodules.passengers.events.DoorOpeningEvent;
import simulator.elevatormodules.passengers.events.MotionEvent;
import simulator.elevatormodules.passengers.events.OverweightBuzzerEvent;
import simulator.elevatormodules.passengers.events.PositionIndicatorChangedEvent;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.ReplicationComputer;

/**
 * Passenger model
 * @author Justin Ray
 *
 * This class models passenger behavior as a series of PassengerAction objects.
 * Each action is scheduled for a future time.  When an action executes, it may create
 * other actions.  This is called an action chain.  Actions may also be initiated
 * by passenger events (through the PassengerEventReceiver interface).
 *
 * The passenger has a satisfaction score object which records deductions in satisfaction
 * for undesirable elevator behavior.
 *
 * The passenger has a PassengerInfo object which contains all the passenger's unique
 * characteristics.
 *
 * The passenger gets a reference to the PassengerControl class, which contains all the
 * system objets it interacts with.  
 *
 * Because the passenger is very complex, good documentation is essential to keeping
 * this class in maintainable state for future TAs.  Therefore, the following documentation
 * conventions MUST be followed when modifying this class:
 *
 * Action classes must have a top-level javadoc comment that describes:
 * -what satisfaction deductions the class may create, and under what conditions
 * -what actions this action may initiate
 * -what the termination condition.  Termination occurs when the action does not create a new action.
 *
 * Actions should fall into one of two categories:
 * - a CallAction - current action related to button pressing
 * - a DoorAction - current action related to entering and exiting the car
 *
 * There should only be one action of each type at any given time, and the current action
 * should be stored in the Pending*Action references doorAction and callAction, so
 * that other actions can access the current action if needed.
 *
 * Recommendations for implementing actions:
 * -Always include runtime checks to verify assumptions about the current system
 * state.  You should AT LEAST verify that the state variable has the
 * expected value.  You should also check the current floor.  It's okay for
 * these checks to throw a runtime exception since they represent a bug in the
 * passenger design.  Try to make the exception description unique and descriptive,
 * as this will help you if the students find the bug.
 *
 * the PassengerEventReceiver inteface allows the passenger to respond to events
 * that are created by the system objects (e.g. a door opening, the car starting to
 * move, etc).  The event behavior is tied up with the implementation of PassengerHandler,
 * since that class decides which events to pass on (based on presence in hall and car queues).
 * See the documentation of that class for more on events.  Thorough documentation
 * of the passengerReceive method is just as important as documentation for Actions.
 *
 * Bug Fixes
 * 20110429 JDR - resolved three bugs:
 *   1. Passenger not pressing car call buttons - there was a corner case where
 * a door re-opening event at the start floor would cancel the
 * CarCallCheckAction and not restart it once the doors were closed.
 * 2. Invalid overweight state exception
 * This was actually two different bugs in corner cases:
 * a) If a passenger is entering and exiting on the same floor (e.g. BACK->FRONT)
 * and causes the car to become overweight, the passenger was not being removed
 * from the door queue for the exit door before attempting to return through the
 * start door.  Arguably, it makes more sense for the passenger to exit through
 * the ending door, but having the passenger go back through the start door is
 * more consistent with the general behavior and avoids having to add a lot of
 * code to handle this one corner case.
 *
 * b) If passenger A is entering one door while passenger B is exiting the other
 * door (already blocking the door) and the car becomes overweight, Passenger A
 * will be signaled to exit (to resolve the overweight condition), but before he
 * can exit, Passenger B may complete his exit and cause the car to no longer be
 * overweight.  I added code to transition Passenger A from exiting due to 
 * overweight to normal waiting in car behavior to address the problem.
 *
 */
public class Passenger implements PassengerEventReceiver, Comparable<Passenger> {

    public final static int DEFAULT_WEIGHT = 1500; //150 lbs
    public final static SimTime DOOR_CHECK_PERIOD = new SimTime(100, SimTime.SimTimeUnit.MILLISECOND);
    public final static SimTime CALL_CHECK_PERIOD = new SimTime(200, SimTime.SimTimeUnit.MILLISECOND);
    public final static SimTime CALL_RECHECK_PERIOD = new SimTime(500, SimTime.SimTimeUnit.MILLISECOND);
    public final static SimTime NULL_PERIOD = new SimTime(1, SimTime.SimTimeUnit.MILLISECOND);
    public final static SimTime BACKOFF_PERIOD = new SimTime(10, SimTime.SimTimeUnit.SECOND);
    //constants for satisfaction score deductions
    private final static double WRONG_MOTION_SCORE = 0.5;
    private final static double ABORT_SCORE = 0.8;
    private final static double REPEAT_PRESS_SCORE = 0.9;
    private final static double FAIL_TO_EXIT_SCORE = 0.5;
    private final static double CAR_LANTERN_CHANGE_SCORE = 0.8;
    private final static double SKIP_DESTINATION_FLOOR_SCORE = 0.5;
    private final static double MISSED_OPENINGS_THRESH_EXCEEDED = 0.5;
    private final static boolean IGNORE_LEVELING = Elevator.getIgnoreLeveling();

    public int compareTo(Passenger o) {
        return info.injectionTime.compareTo(o.info.injectionTime);
    }

    public static enum State {

        INIT,
        WAITING_IN_HALL,
        ENTERING,
        ENTER_BACKOUT,
        WAITING_IN_CAR,
        EXITING,
        EXIT_BACKOUT,
        OVERWEIGHT_EXITING,
        OVERWEIGHT_BACKOFF,
        DONE
    }
    private static int nextPassengerIndex = 0;
    private final int index;
    private final String name;
    private final PassengerControl pc;
    private final boolean verbose;
    //private final Timer timer;
    private PassengerHandler passengerHandler;
    private final PassengerInfo info;
    private PendingAction<DoorAction> doorAction = new PendingAction<DoorAction>(); //reference to the current door action, e.g. entering, exiting
    private PendingAction<CallAction> callAction = new PendingAction<CallAction>(); //reference to the current call action
    //passenger current state values
    private State state;
    private boolean isQueued = false;
    private int carPressCount = 0;
    private int hallPressCount = 0;
    private int missedOpenings = 0;
    private int abortCount = 0; //how many times the passenger failed to traverse a door
    private Direction expectedDirection = Direction.STOP; //holds the direction the passenger expects to move after departing
    private SimTime deliveryTime = null;
    private PassengerSatisfaction satisfaction = new PassengerSatisfaction();

    public Passenger(PassengerControl pc, PassengerInfo info, boolean verbose) {
        index = nextPassengerIndex++;
        name = "Passenger " + index;
        this.pc = pc;
        this.info = info;
        this.verbose = verbose;
    }

    public PassengerInfo getInfo() {
        return info;
    }

    public State getState() {
        return state;
    }

    public String getStatusLine() {
        if (state == State.DONE) {
            return name + ": INJECTED AT " + info.injectionTime + " and DELIVERED in " + deliveryTime;
        } else {
            return name + ": NOT_DELIVERED, " + state;
        }
    }

    public String getInfoLine() {
        //create a string with passenger info;
        return name + " injected at " + info.injectionTime + " " + info.startFloor + "," + info.startHallway + " --> " + info.endFloor + "," + info.endHallway;
    }
    private String toolTipStartStr = null; //for caching tool tip line

    /**
     * @return a line giving a passenger summary for use in the gui
     */
    public String getToolTipLine() {
        //the first part of the tool tip string doesn't change, so cache it.
        if (toolTipStartStr == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(" | ");
            sb.append(info.injectionTime);
            sb.append(" | ");
            sb.append(info.startFloor);
            sb.append(",");
            sb.append(info.startHallway);
            sb.append("->");
            sb.append(info.endFloor);
            sb.append(",");
            sb.append(info.endHallway);
            sb.append(" | ");
            toolTipStartStr = sb.toString();
        }
        return toolTipStartStr + state;
    }

    public void setPassengerHandler(PassengerHandler passengerHandler) {
        this.passengerHandler = passengerHandler;
    }

    public void start() {
        if (info.startFloor == 0) {
            state = State.WAITING_IN_CAR;
            callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
            log("Starting in car");
        } else {
            state = State.WAITING_IN_HALL;
            callAction.set(new HallCallCheckAction(CALL_CHECK_PERIOD));
            log("Starting at landing ", info.startFloor, ", ", info.startHallway);
        }
    }

    //call this function when the last action has been done
    private void finish() {
        deliveryTime = SimTime.subtract(Harness.getTime(), info.injectionTime);
        state = State.DONE;
        //cancel any pending action
        doorAction.cancel();
        callAction.cancel();
        //log any final info
        log("passenger delivered in ", deliveryTime, " to ", info.endFloor, ", ", info.endHallway);
        //tell the handler we are finished
        passengerHandler.passengerFinished(this);

    }

    /**
     * Respond to passenger events by modifying the actions in progress depending
     * on the current state and the event type.
     *
     * Deductions:
     * WRONG_MOTION_SCORE if the car moves in a direction inconsistent with 
     *   expectedDirection
     * SKIPPED_DESTINATION_FLOOR_SCORE if we pass the passenger's destination
     *   without stopping while they are in the car
     *
     * Initiates:
     * CheckHallDoorAction if there is a DoorOpeningEvent while we are in WAITING_IN_HALL
     * CheckCarDoorAction if there is a DoorOpeningEvent while we are in WAITING_IN_CAR
     * OverweightExitAction - if there is an OverweightBuzzerEvent while in the car
     *   note that we always exit in this case, so the OverweightBuzzerEvent should
     *   only be delivered to the last passenger in the car queue by PassengerHandler
     * OverweightBackoffAction - if we are in the hallway and we get an OverweightBuzzerEvent
     *   all passengers in the hall(s) where doors are at least partially open
     *   at the current floor should receive this event.
     * 
     * Other events responded to:
     * MotionEvent - check the direction against our expected direction - no PassengerAction 
     *   initiated.
     * PositionIndicatorChangedEvent - check to see if we passed a passenger's destination
     * while they are in the car.
     *
     *
     * @param e
     */
    public void passengerEvent(PassengerEvent e) {
        //respond to events
        if (e instanceof DoorOpeningEvent) {
            DoorOpeningEvent de = (DoorOpeningEvent) e;
            if (state == State.WAITING_IN_HALL) {
                if (de.getHallway() != info.startHallway) {
                    throw new RuntimeException(Passenger.this.toString() + ":  Not expecting a door open event from hall other than starting hall");
                }
                if (pc.driveMonitor.getCurrentFloor() != info.startFloor) {
                    throw new RuntimeException(Passenger.this.toString() + ":  Not expecting a door open event from a floor other than the starting floor");
                }
                log("Checking door from hall ", info.startFloor, ", ", info.startHallway);
                doorAction.set(new CheckHallDoorAction(DOOR_CHECK_PERIOD));
                callAction.cancel(); //cancel the car call while the door is open
            } else if (state == State.WAITING_IN_CAR) {
                //for passengers in the car, only respond to door events for the door they are interested in
                if (de.getHallway() == info.endHallway) {
                    log("Checking door " + info.endHallway + " from car");
                    doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
                    callAction.cancel(); //cancel the car call while the door is open
                }
            } else {
                log("DoorEvent ignored because we are in state " + state);
                //throw new RuntimeException(Passenger.this.toString() + ":  Did not expect a door event while in state " + state);
            }
        } else if (e instanceof MotionEvent) {
            MotionEvent me = (MotionEvent) e;
            if (expectedDirection != Direction.STOP && expectedDirection != me.getDirection()) {
                String message = "Expected to move " + expectedDirection + " but moved " + me.getDirection();
                satisfaction.addDeduction(WRONG_MOTION_SCORE, message);
                log(message);
            }
        } else if (e instanceof OverweightBuzzerEvent) {
            switch (state) {
                case WAITING_IN_CAR:
                    //passengerHandler is only supposed to send this message
                    //to the last passenger in the car queue, so always exit
                    //if we are in the car and receive it.
                    state = State.OVERWEIGHT_EXITING;

                    doorAction.set(new OverweightExitStartAction(DOOR_CHECK_PERIOD));
                    callAction.cancel();
                    break;
                case WAITING_IN_HALL:
                    state = state.OVERWEIGHT_BACKOFF;
                    doorAction.set(new OverweightBackoffAction(DOOR_CHECK_PERIOD));
                    callAction.cancel();
                    break;
                case ENTER_BACKOUT:
                case ENTERING:
                case EXITING:
                case EXIT_BACKOUT:
                case OVERWEIGHT_BACKOFF:
                case OVERWEIGHT_EXITING:
                    //do nothing
                    break;
                default:
                    throw new RuntimeException(Passenger.this.toString() + ":  passenger did not expect to receive an overweight event in state " + state);
            }
        } else if (e instanceof PositionIndicatorChangedEvent) {
            //only check this event if we are waiting in the car.
            PositionIndicatorChangedEvent pice = (PositionIndicatorChangedEvent) e;
            if (state == State.WAITING_IN_CAR) {
                if (pice.getPreviousPosition() == info.endFloor && pc.driveMonitor.getDriveState() == DriveState.MOVING) {
                    satisfaction.addDeduction(SKIP_DESTINATION_FLOOR_SCORE, "Car passed passenger's destination floor without servicing it");
                }
            }
        }
    }

    /**
     * Return true if the floor and hall are the start floor and hall
     * and the direction is consistent with our direction of travel
     * @param floor
     * @param hallway
     * @param direction
     * @return
     */
    boolean mightEnter(int floor, Hallway hallway, Direction direction) {
        return (floor == info.startFloor && hallway == info.startHallway && checkHallDirection(pc.carLanterns.getLanternDirection()));
    }

    /**
     * return true if the floor and hall are where we want to exit
     */
    boolean mightExit(int floor, Hallway hallway) {
        return (floor == info.endFloor && hallway == info.endHallway);
    }

    SimTime getDeliveryTime() {
        if (state != State.DONE) {
            return SimTime.FOREVER;
        }
        return deliveryTime;
    }

    double getSatisfactionScore() {
        return satisfaction.getScore();
    }

    String getSatisfactionStats() {
        return satisfaction.toString();
    }

    int getWeight() {
        return DEFAULT_WEIGHT;
    }

    private void log(Object... msg) {
        if (verbose) {
            Harness.log(name, msg);
        }
    }

    @Override
    public String toString() {
        //create a string with passenger info;
        return name;
    }

    /**
     * @param d a direction value to test (probably the current lantern direction)
     * @return true if d is consistent with the direction we want to travel or if
     * we have missed so many door openings that we want to give up waiting for the
     * right direction.
     */
    private boolean checkHallDirection(Direction d) {
        //if we've missed too many openings, then we get on no matter what direction
        //the car is traveling in
        if (missedOpenings > info.missedOpeningThreshold) {
            return true;
        }
        //if passenger direction is stop, then always compatible.
        if (info.travelDirection == Direction.STOP) {
            return true;
        }
        //if the lantern direction is stop, always compatible
        if (d == Direction.STOP) {
            return true;
        }
        //compatible if directions match
        if (d == info.travelDirection) {
            return true;
        }
        //otherwise not compatible;
        return false;
    }

    /**************************************************************************
     * Door Actions
     *
     * These actions affect the passenger position (hall or car) and interactions
     * with the doors.
     **************************************************************************/
    /**
     * Door actions should be descended from this class
     */
    private abstract class DoorAction extends PassengerAction {

        public DoorAction(SimTime offset) {
            super(offset);
        }
    }

    /**
     * This is the action passengers take while waiting at the start hall.
     * Passenger watches for the direction arrow to be consistent with their intended
     * direction and gets into the door queue if that happens.
     * Once it is this passengers turn to enter, blocks the doors.
     * Also saves expectedDirection, the lantern direction that caused the passenger to enter.
     * 
     * Can initiate:
     * EnterCarAction (if the direction is consistent and it is next in line)
     * CheckHallDoorAction (if entering conditions are not met, but doors are still open)
     * 
     * Terminates when doors close.
     */
    private class CheckHallDoorAction extends DoorAction {

        public CheckHallDoorAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            //log(this.toString(), " running at ", Harness.getTime());
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            if (state != State.WAITING_IN_HALL) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }


            //monitor the arrows to see if this is the way we want to go
            if (checkHallDirection(pc.carLanterns.getLanternDirection()) && checkFloor()) {
                //direction and floor are consistent
                //queue if we are not already queued
                if (!isQueued) {
                    passengerHandler.getDoorQueue(info.startHallway).requestEnter(Passenger.this);
                    isQueued = true;
                }
            } else {
                //direction and/or floor are not consistent
                if (isQueued) {
                    //direction is inconsistent, so exit the door queue --
                    //this happens if the lanterns come on in the wrong direction
                    //after the door starts opening
                    passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
                    isQueued = false;
                }
            }


            //check the door width
            //check car level
            //check for priority in queue
            if ((pc.driveMonitor.isLevel() || IGNORE_LEVELING)
                    && isQueued
                    && theDoor.getWidth() >= info.width
                    && passengerHandler.getDoorQueue(info.startHallway).isNext(Passenger.this)) {
                //block the doors and try to enter
                log("Blocking door ", info.startHallway, "(", info.width, ")");
                if (theDoor.block(info.width)) {
                    log("Attempting to enter car from ", info.startFloor, ",", info.startHallway);

                    state = State.ENTERING;
                    doorAction.set(new FinishCarEnterAction(info.doorTraversalDelay));
                    //set expected direction to the lantern value
                    expectedDirection = pc.carLanterns.getLanternDirection();
                } else {
                    //someone else has blocked the door, so try again later
                    doorAction.set(new CheckHallDoorAction(DOOR_CHECK_PERIOD));
                }
            } else {
                //condition for entering not met
                if (theDoor.isNotClosed()) {
                    //while doors are open, keep reissuing the check action
                    doorAction.set(new CheckHallDoorAction(DOOR_CHECK_PERIOD));
                } else {

                    if (isQueued) {
                        //since it was queued, we wanted to get on, but were unable to
                        //this can happen if a wide passenger blocks other passengers from
                        //entering
                        isQueued = false;
                        passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
                    } else {
                        //doors closed, so we missed an opening because it wasn't in our direction
                        missedOpenings++;
                        log("Doors opened at ", info.startFloor, ",", info.startHallway, " but direction was never consistent with ", info.travelDirection, ".  Missed count=", missedOpenings);
                    }
                    //don't schedule any door events - more will come if the door opens
                    //restart the hall call checking after BACKOFF period
                    callAction.set(new HallCallCheckAction(BACKOFF_PERIOD));

                }
            }
        }

        private boolean checkFloor() {
            return (pc.driveMonitor.getCurrentFloor() == info.startFloor);
        }

        @Override
        public String toString() {
            return "CheckHallDoorAction";
        }
    }

    /**
     * Check door width and enter car if the door are still open wide enough,
     * otherwise back out and try again.
     * Once in the car, initiate car door checking if the end floor is also
     * the current floor, otherwise initiate lantern checking.
     *
     * Deductions:
     * ABORT_SCORE if we fail to enter the car
     *
     * Initiates:
     * CarCallCheckAction - always does this so the passenger will press and monitor the car call
     * CheckCarDoorAction - after entering, if the current floor is also the endFloor
     * CheckLanternDirectionAction - after entering, if the current floor is not the endFloor.
     * EnterAbortAction - if the doors are not wide enough to enter.
     * (no termination condition because not a looping action)
     */
    private class FinishCarEnterAction extends DoorAction {

        public FinishCarEnterAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.ENTERING) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            //check the door width
            if (theDoor.getWidth() >= info.width) {
                passengerHandler.joinCarQueue(Passenger.this);
                passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
                isQueued = false;
                log("Unblocking door ", info.startHallway, "(", info.width, ")");
                theDoor.unblock();
                state = State.WAITING_IN_CAR;
                log("Entered the car from ", info.startFloor, ", ", info.startHallway);
                //check missed count
                if (missedOpenings > info.missedOpeningThreshold) {
                    String msg = "After the car came by " + missedOpenings + " times without showing arrows compatible with " + info.travelDirection + ", finally gave up and got on anyway.";
                    satisfaction.addDeduction(MISSED_OPENINGS_THRESH_EXCEEDED, msg);
                    // Since we are getting on regardless of lantern, we don't care about direction anymore
                    expectedDirection = Direction.STOP;
                    log(msg);
                }

                /*
                 * JDR-20110429
                 * Note:  These same actions are also issued in a special case
                 * in OverweightExitStartAction.execute()  If a bug fix is ever
                 * made here, it should also be made there.
                 */
                //start the next action depending on our end floor
                if (pc.driveMonitor.getCurrentFloor() == info.endFloor) {
                    //end floor same as start, so initiate door checking
                    doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
                } else {
                    //end floor is somewhere else, so initiate lantern checking
                    doorAction.set(new CheckLanternDirectionAction(DOOR_CHECK_PERIOD));
                }
                //alwasy start the car call check action
                callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
            } else {
                //doors not wide enough to enter
                abortCount++;
                satisfaction.addDeduction(ABORT_SCORE, "Failed to enter car");
                log("Backing out at ", info.startFloor, ", ", info.startHallway, " - ", abortCount, " total failed door traverals");
                state = State.ENTER_BACKOUT;
                doorAction.set(new EnterAbortAction(info.doorBackoutDelay));
            }
        }

        @Override
        public String toString() {
            return "EnterCarAction";
        }
    }

    /**
     * Monitor the car lanterns to make sure they stay consistent with our intended
     * direction.
     *
     * Deductions:
     * CAR_LANTERN_CHANGE_SCORE if the lantern direction changes
     * 
     * Initiates:
     * BeginExitAtStartAction - if the lantern changes to an inconsistent direction
     * CheckLenternDirectionAction - if the lantern is consistent, to keep checking 
     * terminates when doors close.
     */
    private class CheckLanternDirectionAction extends DoorAction {

        public CheckLanternDirectionAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            //log(this.toString(), " running at ", Harness.getTime());
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            if (state != State.WAITING_IN_CAR) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            //see if the direction arrow remains consistent with expected direction
            if (expectedDirection != Direction.STOP && pc.carLanterns.getLanternDirection() != info.travelDirection) {
                //the lantern has changed, so exit the car at the start floor
                doorAction.set(new BeginExitAtStartAction(NULL_PERIOD));
                log("Expected direction was ", expectedDirection, " but the lantern changed to ", pc.carLanterns.getLanternDirection(), " so exiting.");
                satisfaction.addDeduction(CAR_LANTERN_CHANGE_SCORE, "Car lantern changed from " + expectedDirection + " to " + pc.carLanterns.getLanternDirection() + " after entering the car.");
            } else {
                if (theDoor.isNotClosed()) {
                    //the lantern is consistent, so keep checking but otherwise do nothing
                    doorAction.set(new CheckLanternDirectionAction(DOOR_CHECK_PERIOD));
                } //else doors are closed, so schedule no new action.
            }
        }

        @Override
        public String toString() {
            return "CheckDirectionAction";
        }
    }

    /**
     * Get into the door queue and exit at the startHallway.  This action assumes
     * we are already at the start floor.
     *
     * Deductions:
     * none
     *
     * initiates:
     * BeginExitAtStartAction - if door exit conditions are not met, to keep trying to exit
     * FinishExitAtStartAction - after blocking the doors when it is this passengers turn to exit.
     *
     * teminates when doors close -- we give up on trying to exit
     */
    private class BeginExitAtStartAction extends DoorAction {

        public BeginExitAtStartAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.WAITING_IN_CAR) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            if (pc.driveMonitor.getCurrentFloor() != info.startFloor) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Did not expect BeginExitAtStartAction at a floor other than the start floor.");
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            //if the current floor is not our destination, then we don't need to keep

            //ready to exit, so get into the door queue.
            if (!isQueued) {
                passengerHandler.getDoorQueue(info.startHallway).requestExit(Passenger.this);
                isQueued = true;
            }

            //check the door width
            //check for priority in queue
            if ((pc.driveMonitor.isLevel() || IGNORE_LEVELING)
                    && isQueued
                    && theDoor.getWidth() >= info.width
                    && passengerHandler.getDoorQueue(info.startHallway).isNext(Passenger.this)) {
                //block the doors and try to enter
                log("Blocking door ", info.startHallway, "(", info.width, ")");
                if (!theDoor.block(info.width)) {
                    state = State.EXITING;
                    doorAction.set(new FinishExitAtStartAction(info.doorTraversalDelay));
                    log("Attempting to exit car at ", info.startFloor, ", ", info.startHallway);
                } else {
                    //someone else has blocked the door, so wait a while and try again
                    doorAction.set(new BeginExitAtStartAction(DOOR_CHECK_PERIOD));
                }
            } else {
                if (theDoor.isNotClosed()) {
                    //while doors are open, keep reissuing the check action
                    doorAction.set(new BeginExitAtStartAction(DOOR_CHECK_PERIOD));
                } else {
                    //doors closed, so we missed our chance to exit.
                    //terminate action sequence
                }
            }
        }

        @Override
        public String toString() {
            return "BeginExitAtStartAction";
        }
    }

    /**
     * Finish exiting at the start floor and put the passenger back in the hall queue,
     * but don't restart the door checking action -- this creates a performance penalty,
     * because the passenger will wait until the next arrival to try enter.
     *
     * Deductions:
     * ABORT_SCORE if we fail to exit
     *
     * Initiates:
     * HallCallCheckAction - to start pressing the hall button, if the exit is successful.
     * ExitAbortAction - if the doors are not open wide enough to exit
     * this is not a looping action, so no termination needed
     */
    private class FinishExitAtStartAction extends DoorAction {

        public FinishExitAtStartAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {

            if (state != State.EXITING) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            if (pc.driveMonitor.getCurrentFloor() != info.startFloor) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Did not expect FinishExitAtStartAction at a floor other than the start floor.");
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            //check the door width
            if (theDoor.getWidth() >= info.width) {
                log("Unblocking door ", info.startHallway, "(", info.width, ")");
                passengerHandler.joinHallQueue(Passenger.this, info.startFloor, info.startHallway);
                theDoor.unblock();
                passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
                isQueued = false;
                state = State.WAITING_IN_HALL;
                callAction.set(new HallCallCheckAction(CALL_CHECK_PERIOD));
                log("Exited car at START FLOOR ", info.startFloor, ",", info.startHallway);
            } else {
                //doors not wide enough to enter
                abortCount++;
                satisfaction.addDeduction(ABORT_SCORE, "Failed to exit car at " + info.startFloor + "," + info.startHallway);
                log("Failed to exit car at ", info.startFloor, ",", info.startHallway, " - ", abortCount, " total failed door traverals");
                state = State.EXIT_BACKOUT;
                doorAction.set(new ExitAtStartAbortAction(info.doorBackoutDelay));
            }
        }

        @Override
        public String toString() {
            return "ExitCarAction";
        }
    }

    /**
     * Usually occurs if we failed to exit the car because the doors weren't open
     * wide enough.  Re-enter the car queue and try to start over with exiting the car
     * at the start floor.
     *
     * Deductions:
     * None
     *
     * Initiates:
     * BeginExitAtStartAction - always
     */
    private class ExitAtStartAbortAction extends DoorAction {

        public ExitAtStartAbortAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.EXIT_BACKOUT) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            log("Unblocking door ", info.startHallway, "(", info.width, ")");
            theDoor.unblock();
            passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
            isQueued = false;
            //return to the car
            log("Returned to the car after failing to exit at ", info.startFloor, ",", info.startHallway);
            state = State.WAITING_IN_CAR;
            //move to the end of the line
            passengerHandler.requeue(Passenger.this);
            //restart the car exit action
            doorAction.set(new BeginExitAtStartAction(DOOR_CHECK_PERIOD));
        }

        @Override
        public String toString() {
            return "ExitAtStartAbortAction";
        }
    }

    /**
     * Occurs if we failed to enter the car.  Reenter the hall queue and resume
     * checking the door and hall call.
     *
     * Deductions:
     * None
     *
     * Initiates:
     * CheckHallDoorAction - always
     * CheckHallCallAction - always
     */
    private class EnterAbortAction extends DoorAction {

        public EnterAbortAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.ENTER_BACKOUT) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            log("Unblocking door ", info.startHallway, "(", info.width, ")");
            theDoor.unblock();
            passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
            isQueued = false;
            //return to the hall
            log("Returned to hall landing ", info.startFloor, ", ", info.startHallway, " after failing to enter.");
            state = State.WAITING_IN_HALL;
            //move to the end of the line
            passengerHandler.requeue(Passenger.this);
            //restart with hall door checking
            doorAction.set(new CheckHallDoorAction(DOOR_CHECK_PERIOD));
            callAction.set(new HallCallCheckAction(CALL_CHECK_PERIOD));
            hallPressCount = 0;
        }

        @Override
        public String toString() {
            return "EnterAbortAction";
        }
    }

    /**
     * Monitor the car door to see if we can exit the car.  If the floor is the
     * endFloor and the exit conditions are met, then block the doors and begin
     * exiting.
     * 
     * Deductions:
     * FAIL_TO_EXIT_SCORE if we fail to exit even though the doors opened
     * 
     * Initiates:
     * ExitCarAction - if the exit conditions are met.
     * CheckCarDoorAction - if the exit conditions are not met, to keep checking
     * CarCallCheckAction - if we fail to exit and the doors close, so that the passenger will re-issue car calls
     * 
     * Terminates immediately if the current floor is not our end floor, or when
     * the doors close.
     */
    private class CheckCarDoorAction extends DoorAction {

        public CheckCarDoorAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.WAITING_IN_CAR) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.endHallway)];
            //if the current floor is not our destination, then we don't need to keep
            //checking the door
            if (!checkFloor()) {
                //end action without starting any new action.
                /*
                JDR-20110429

                Restart the callAction to fix a bug where the passenger stops
                pressing his button.

                This can happen if the doors reopen at the entrance floor
                (causing a DoorOpen event, which triggers a CheckCarDoorAction
                and cancels the CarCallCheckAction. */
                callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
                return;
            }

            //if we get here, then we are ready to exit, so get into the door queue.
            if (!isQueued) {
                passengerHandler.getDoorQueue(info.endHallway).requestExit(Passenger.this);
                isQueued = true;
            }


            //check the door width
            //check the current floor
            //check for priority in queue
            if ((pc.driveMonitor.isLevel() || IGNORE_LEVELING)
                    && isQueued
                    && theDoor.getWidth() >= info.width
                    && passengerHandler.getDoorQueue(info.endHallway).isNext(Passenger.this)) {
                //block the doors and try to enter
                log("Blocking door ", info.endHallway, "(", info.width, ")");
                if (!theDoor.block(info.width)) {
                    state = State.EXITING;
                    doorAction.set(new ExitCarAction(info.doorTraversalDelay));
                    log("Attempting to exit car at ", info.endFloor, ", ", info.endHallway);
                } else {
                    //someone else has blocked the door, so wait a while and try again
                    doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
                }
            } else {
                if (theDoor.isNotClosed()) {
                    //while doors are open, keep reissuing the check action
                    doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
                } else {
                    //doors closed, so we missed our chance to exit
                    missedOpenings++;
                    log("Doors opened at ", info.endFloor, ", ", info.endHallway, " but unable to exit.");
                    satisfaction.addDeduction(FAIL_TO_EXIT_SCORE, "Failed to exit at " + info.endFloor + ", " + info.endHallway + " even though the doors opened.");
                    if (isQueued) {
                        isQueued = false;
                        passengerHandler.getDoorQueue(info.endHallway).remove(Passenger.this);
                    }
                    //since we missed our chance to get out, we no longer care about elevator direction
                    expectedDirection = Direction.STOP;
                    //don't schedule any door more events - more will come if the door opens
                    //restart car call
                    callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
                }
            }
        }

        private boolean checkFloor() {
            return (pc.carPositionIndicator.getIndicatedFloor() == info.endFloor);
        }

        @Override
        public String toString() {
            return "CheckCarDoorAction";
        }
    }

    /**
     * Exit at the endFloor and execute the finish call for this passenger
     * 
     * Deductions:
     * ABORT_SCORE if we fail to exit
     * 
     * Initiates:
     * ExitAbortAction - if the doors are not open wide enough
     * otherwise, no actions initiated because passenger is done.
     */
    private class ExitCarAction extends DoorAction {

        public ExitCarAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {

            if (state != State.EXITING) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.endHallway)];
            //check the door width
            if (theDoor.getWidth() >= info.width) {
                log("Unblocking door ", info.endHallway, "(", info.width, ")");
                theDoor.unblock();
                passengerHandler.getDoorQueue(info.endHallway).remove(Passenger.this);
                isQueued = false;
                state = State.DONE;
                log("Successfully exited car at ", info.endFloor, ",", info.endHallway);
                finish();
            } else {
                //doors not wide enough to enter
                abortCount++;
                satisfaction.addDeduction(ABORT_SCORE, "Failed to exit car at " + info.endFloor + "," + info.endHallway);
                log("Failed to exit car at ", info.endFloor, ",", info.endHallway, " - ", abortCount, " total failed door traverals");
                state = State.EXIT_BACKOUT;
                doorAction.set(new ExitAbortAction(info.doorBackoutDelay));
            }
        }

        @Override
        public String toString() {
            return "ExitCarAction";
        }
    }

    /**
     * Occurs when the passenger fails to exit.  
     * 
     * Deductions:
     * none
     * 
     * Initiates:
     * CheckCarDoorAction - always
     * CheckCarCallAction - always
     * no termination condition because this is not a looping action
     */
    private class ExitAbortAction extends DoorAction {

        public ExitAbortAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.EXIT_BACKOUT) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.endHallway)];
            log("Unblocking door ", info.endHallway, "(", info.width, ")");
            theDoor.unblock();
            passengerHandler.getDoorQueue(info.endHallway).remove(Passenger.this);
            isQueued = false;
            //return to the car
            log("Returned to the car after failing to exit at ", info.endFloor, ",", info.endHallway);
            state = State.WAITING_IN_CAR;
            //move to the end of the line
            passengerHandler.requeue(Passenger.this);
            //restart with hall door checking
            doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
            callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
            carPressCount = 0;
        }

        @Override
        public String toString() {
            return "ExitAbortAction";
        }
    }

    /**
     * Enter the door queue to monitor the doors and block the door
     * when it is our turn to exit.
     * 
     * Deductions:
     * none
     * 
     * Initiates:
     * OverweightExitFinishAction - after we block the doors
     * OverWeightExitStartAction - if the door traversal conditions are not met
     *
     * never terminates, because a correct elevator cannot do anything until the 
     * passenger exits.
     */
    private class OverweightExitStartAction extends DoorAction {

        public OverweightExitStartAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.OVERWEIGHT_EXITING) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            //sanity check the current state
            if (pc.carPositionIndicator.getIndicatedFloor() != info.startFloor) {
                throw new RuntimeException(Passenger.this.toString() + ":  unexpectedly running OverWeightExitStartAction while not at the start floor");
            }
            /*
             * JDR-20110429
             * Added this check make sure that the passenger is not in a door queue
             * for other than the start door - this covers a corner case for
             * a passenger entering on one side of the elevator and exiting on
             * the other side at the same floor when an overweight condition
             * occurs.
             */
            if (isQueued && !passengerHandler.getDoorQueue(info.startHallway).contains(Passenger.this)) {
                passengerHandler.getDoorQueue(Hallway.BACK).remove(Passenger.this);
                passengerHandler.getDoorQueue(Hallway.FRONT).remove(Passenger.this);
                isQueued = false;
            }
            if (!isQueued) {
                passengerHandler.getDoorQueue(info.startHallway).requestExit(Passenger.this);
                isQueued = true;
            }

            //check the door width
            //check the current floor
            //check for priority in queue
            if ((pc.driveMonitor.isLevel() || IGNORE_LEVELING)
                    && isQueued
                    && theDoor.getWidth() >= info.width
                    && passengerHandler.getDoorQueue(info.startHallway).isNext(Passenger.this)) {

                //block the doors and try to enter
                log("Blocking door ", info.startHallway, "(", info.width, ")");
                if (theDoor.block(info.width)) {
                    state = State.OVERWEIGHT_EXITING;
                    doorAction.set(new OverweightExitFinishAction(info.doorTraversalDelay));
                    log("Attempting to exit overweight car at ", info.startFloor, ",", info.startHallway);
                } else {
                    //someone else has blocked the door, so wait a little while and try again
                    doorAction.set(new OverweightExitStartAction(DOOR_CHECK_PERIOD));
                }
            } else if (!pc.carWeightAlarm.isRinging()) {
                /*
                 * JDR-20110429
                 * added this condition to resolve an issue where a passenger
                 * enters the car from one door while a passenger is already in
                 * the process of exiting from another door and the car is almost
                 * overweight.
                 * This case allows the passenger to transition back to WaitingInCar
                 * if the car is no longer overweight
                 *
                 * Note that these are the same actions issued at the end of FinishCarEnterAction
                 */
                //set new state
                state = State.WAITING_IN_CAR;
                //dequeue
                if (isQueued) {
                    passengerHandler.getDoorQueue(Hallway.FRONT).remove(Passenger.this);
                    passengerHandler.getDoorQueue(Hallway.BACK).remove(Passenger.this);
                    isQueued = false;
                }
                //start the next action depending on our end floor
                if (pc.driveMonitor.getCurrentFloor() == info.endFloor) {
                    //end floor same as start, so initiate door checking
                    doorAction.set(new CheckCarDoorAction(DOOR_CHECK_PERIOD));
                } else {
                    //end floor is somewhere else, so initiate lantern checking
                    doorAction.set(new CheckLanternDirectionAction(DOOR_CHECK_PERIOD));
                }
                //alwasy start the car call check action
                callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
            } else {
                //conditions not met, so keep reissuing the check action
                doorAction.set(new OverweightExitStartAction(DOOR_CHECK_PERIOD));
            }
        }

        @Override
        public String toString() {
            return "CheckCarDoorAction";
        }
    }

    /**
     * Unblock the doors and return to the hall queue.
     * 
     * Deductions:
     * ABORT_SCORE if we fail to exit
     * 
     * Initiates:
     * OverweightExitFinishAction - if the doors aren't open wide enough to exit, keep trying to exit
     *   because the car cannot move until it is no longer overweight
     * OverweightBackoffAction - if we exit successfully
     * 
     * no termination condition because we have to exit if overweight
     */
    private class OverweightExitFinishAction extends DoorAction {

        public OverweightExitFinishAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {

            if (state != State.OVERWEIGHT_EXITING) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            //check the door width
            if (theDoor.getWidth() >= info.width) {
                passengerHandler.joinHallQueue(Passenger.this, info.startFloor, info.startHallway);
                log("Unblocking door ", info.startHallway, "(", info.width, ")");
                theDoor.unblock();
                passengerHandler.getDoorQueue(info.startHallway).remove(Passenger.this);
                isQueued = false;
                state = State.OVERWEIGHT_BACKOFF;
                log("Successfully exited overweight car at ", info.startFloor, ",", info.startHallway);
                doorAction.set(new OverweightBackoffAction(DOOR_CHECK_PERIOD));
                callAction.cancel();
            } else {
                //doors not wide enough to exit
                abortCount++;
                satisfaction.addDeduction(ABORT_SCORE, "Failed to exit overweight car at " + info.startFloor + "," + info.startHallway);
                log("Failed to exit overweight car at ", info.startFloor, ",", info.startHallway, " - ", abortCount, " total failed door traverals");
                //keep trying to exit
                state = State.OVERWEIGHT_EXITING;
                doorAction.set(new OverweightExitFinishAction(info.doorBackoutDelay));
                callAction.cancel();
            }
        }

        @Override
        public String toString() {
            return "OverweightExitCarAction";
        }
    }

    /**
     * Monitor the doors, and stop hall calls for BACKOFF_PERIOD time after the
     * doors close to prevent deadlock when overweight.
     *
     * Deductions:
     * none
     *
     * Initiates:
     * OverweightBackoffAction - if the doors are not closed
     * HallCallCheckAction - after BACKOFF_PERIOD once the doors close.
     *
     * no termination, eventually gets into HallCallCheckAction
     */
    private class OverweightBackoffAction extends DoorAction {

        public OverweightBackoffAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.OVERWEIGHT_BACKOFF) {
                throw new IllegalStateException(Passenger.this.toString() + ":  Invalid state " + state);
            }
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            if (theDoor.isClosed()) {
                //now that the doors are closed, wait for the full backoff period and
                //then return to being in the hall.

                log("Doors are not closed, starting overweight backoff, then returning to hall wait at ", info.startFloor, ",", info.startHallway);
                state = State.WAITING_IN_HALL;

                //do not issue a door check command.  That will come when the doors open at this floor
                //doorAction.set(new CheckHallDoorAction(DOOR_CHECK_PERIOD));

                //restart with hall call checking after the backoff period
                callAction.set(new HallCallCheckAction(BACKOFF_PERIOD));
                hallPressCount = 0;
            } else {
                //reissue the backoff action to keep checking for door completely closed
                callAction.cancel();
                doorAction.set(new OverweightBackoffAction(DOOR_CHECK_PERIOD));

            }
        }

        @Override
        public String toString() {
            return "EnterAbortAction";
        }
    }

    /***************************************************************************
     * Call Actions
     *
     * These actiona affect button press behavior.
     *
     ***************************************************************************/
    /**
     * Call actions should be descended from CallAction
     */
    private abstract class CallAction extends PassengerAction {

        public CallAction(SimTime offset) {
            super(offset);
        }
    }

    /**
     * Periodically check to see if the hall light is lit.  If not, press it.
     *
     * Deductions:
     * REPEAT_PRESS_SCORE if we have to press the button more than once.
     *
     * Initiates:
     * HallCallCheckAction - always, if the passenger is waiting in the hall
     *
     * terminates when the passengers state is no longer WAITING_IN_HALL and
     * must be restarted by some other action.
     */
    private class HallCallCheckAction extends CallAction {

        public HallCallCheckAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.WAITING_IN_HALL) {
                //do nothing if we're not waiting in the hall
                //this also cancels the HallCallCheck, because we do not renew the action
                return;
            }
            HallButtonLight theButton = pc.hallCalls[ReplicationComputer.computeReplicationId(info.startFloor, info.startHallway, info.hallCallDirection)];
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.startHallway)];
            int currentFloor = pc.driveMonitor.getCurrentFloor();

            //see if the button is still lighted
            //but skip the check if we are currently at the floor with the doors open
            if (!theButton.isLighted()
                    && !(currentFloor == info.startFloor && theDoor.isNotClosed())) {
                theButton.press(info.hallPressTime);
                hallPressCount++;
                log("Pressing hall call at ", info.startFloor, ",", info.startHallway, "(", hallPressCount, " presses)");
                if (hallPressCount > 1) {
                    satisfaction.addDeduction(REPEAT_PRESS_SCORE, "Repeated hall call press");
                }
                //use the recheck period since we just pushed the button
                callAction.set(new HallCallCheckAction(CALL_RECHECK_PERIOD));
            } else {
                //check every so often to see if the button is still pressed.
                callAction.set(new HallCallCheckAction(CALL_CHECK_PERIOD));
            }
        }

        @Override
        public String toString() {
            return "HallCallCheckAction";
        }
    }

    /**
     * Periodically check to see if the car light is lit.  Press it if not.
     *
     * Deductions:
     * REPEAT_PRESS_SCORE - if we have to press the button more than once.
     *
     * Initiates:
     * CarCallCheckAction - to keep checking the button
     *
     * terminates if the state is no longer WAITING_IN_CAR and must be restarted
     * by another action.
     */
    private class CarCallCheckAction extends CallAction {

        public CarCallCheckAction(SimTime offset) {
            super(offset);
        }

        @Override
        public void execute() {
            if (state != State.WAITING_IN_CAR) {
                return; //do nothing if we're not waiting in the hall
            }
            CarButtonLight theButton = pc.carCalls[ReplicationComputer.computeReplicationId(info.endFloor, info.endHallway)];
            Door theDoor = pc.doors[ReplicationComputer.computeReplicationId(info.endHallway)];
            //see if the button is still lighted
            //but skip the check if the doors are open
            if (!theButton.isLighted()
                    && theDoor.isClosed()) {
                theButton.press(info.carPressTime);
                carPressCount++;
                log("Pressing car call for ", info.endFloor, ",", info.endHallway, "(", carPressCount, " presses)");
                if (carPressCount > 1) {
                    satisfaction.addDeduction(REPEAT_PRESS_SCORE, "Repeated car call press");
                }
                callAction.set(new CarCallCheckAction(CALL_RECHECK_PERIOD));
            } else {
                callAction.set(new CarCallCheckAction(CALL_CHECK_PERIOD));
            }

        }

        @Override
        public String toString() {
            return "CarCallCheckAction";
        }
    }
}
