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
import simulator.framework.Hallway;
import simulator.framework.RuntimeMonitor;

import jSimPack.SimTime;
import simulator.framework.*;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;



/**
 * Runtime monitor to verify high-level requirements.  Based on SampleDispatcherMonitor
 *
 * See the documentation of simulator.framework.RuntimeMonitor for more details.
 *
 * @author Jesse Salazar
 */
public class Proj11RuntimeMonitor extends RuntimeMonitor {
	
	
	/* Initialize State Machine Vars (includes Proj7RuntimeMonitor.java init) */
	AtFloorStateMachine atFloorState = new AtFloorStateMachine();
    DoorReversalStateMachine doorReversalState = new DoorReversalStateMachine();
    DoorStateMachine doorState = new DoorStateMachine();
    WeightStateMachine weightState = new WeightStateMachine();
	DriveStateMachine driveState = new DriveStateMachine();
	
    Stopwatch doorReversalStopwatch = new Stopwatch();
	
    boolean hasMoved = false;
    boolean wasOverweight = false;
    int overWeightCount = 0;
    int wastedOpeningsCount = 0;
	int stoppedUndesiredCount = 0;
	
    protected int currentFloor = MessageDictionary.NONE;
	protected int desiredFloorFl = MessageDictionary.NONE;
	protected Direction desiredFloorDir = Direction.STOP;
    protected int lastStoppedFloor = MessageDictionary.NONE;
    protected boolean fastSpeedReached = false;
	

	
	
    public Proj11RuntimeMonitor() {
        //initialization goes here
    }
	
	/* MODIFY: THE SUMMARIZE FUNCTION BELOW NEEDS TO BE UPDATED */
	@Override
    protected String[] summarize() {
        String[] arr = new String[4];
        arr[0] = "Overweight count = " + overWeightCount;
        arr[1] = "Wasted openings count = " + wastedOpeningsCount;
        arr[2] = "Time dealing with door reversals = "
		+ doorReversalStopwatch.getAccumulatedTime().toString();
        arr[1] = "Undesired stop count = " + stoppedUndesiredCount;
		return arr;
    }
	
	public void timerExpired(Object callbackData) {
        //implement time-sensitive behaviors here
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
    private void doorOpening(Hallway hallway) {
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
        for (Direction d : Direction.replicationValues) {
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
    private void doorClosing(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Closing");
    }
	
    /**
     * Called once if the door starts opening after it started closing but before
     * it was fully closed.
     * @param hallway which door the event pertains to
     */
    private void doorReopening(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Reopening");
    }
	
    /**
     * Called once when the doors close completely
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
    private void doorOpened(Hallway hallway) {
        //System.out.println(hallway.toString() + " Door Opened");
    }
	
    /**
     * Called when the car weight changes
     * @param newWeight an incoming weight sensor value
     */
    private void weightChanged(int newWeight) {
        //System.out.println("Elevator weight changed to " + newWeight);
        if (newWeight > Elevator.MaxCarCapacity) {
            wasOverweight = true;
        }
    }
	
    /**
     * Called when a new door reversal occurs
     * @param hallway which door the event pertains to
     * @param side which door the event pertains to
     */
    private void doorReversalStarted(Hallway hallway, Side side) {
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
    private void doorReversalEnded(Hallway hallway, Side side) {
        // System.out.println(hallway.toString() + " " + side.toString()
        //         + " Door Reversal Ended");
    }
	
	
	
	
    /**************************************************************************
     * low level message receiving methods
     * 
     * These mostly forward messages to the appropriate state machines
     **************************************************************************/
    @Override
    public void receive(ReadableDoorReversalPayload msg) {
        doorReversalState.receive(msg);
    }
	
    @Override
    public void receive(ReadableDoorClosedPayload msg) {
        doorState.receive(msg);
    }
	
    @Override
    public void receive(ReadableDoorOpenPayload msg) {
        doorState.receive(msg);
    }
	
    @Override
    public void receive(ReadableDoorMotorPayload msg) {
        doorState.receive(msg);
    }
	
    @Override
    public void receive(ReadableCarWeightPayload msg) {
        weightState.receive(msg);
    }
	
    @Override
    public void receive(ReadableDriveSpeedPayload msg) {
        if (msg.speed() > 0) {
            hasMoved = true;
        }
		/* Set driveState based on DriveSpeedPayload msg */
		driveState.receive(msg);
		checkFastSpeed(msg);
    }
 
	@Override
    public void receive(ReadableAtFloorPayload msg) {
        atFloorState.receive(msg);
		updateCurrentFloor(msg);
    }
	
	//@Override
//    public void receive(DesiredFloorCanPayloadTranslator msg) {
//        desiredFloorFl = msg.getFloor();
//		desiredFloorDir = msg.getDirection();
//    }

    /**
     * Warn if the drive was never commanded to fast when fast speed could be
     * used.
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
                        warning("The drive was not commanded to FAST on the trip between " + lastStoppedFloor + " and " + currentFloor);
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
            if (!atFloors[lastAtFloor.getFloor()-1][Hallway.BACK.ordinal()].value() && !atFloors[lastAtFloor.getFloor()-1][Hallway.FRONT.ordinal()].value()) {
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

	
	/* DriveState emumerations for DriveStateMachine */
	private static enum DriveState {
        STOPPED,
        MOVING,
        STOPPED_UNDESIRED
    }
	/**
     * Utility class to detect stops at undesired floors.
     */
    private class DriveStateMachine {

		DriveState driveState;
		
		public DriveStateMachine(){
			driveState = DriveState.STOPPED;
		}	
		
        public void receive(ReadableDriveSpeedPayload msg) {
            updateState(msg.speed());
        }
		
		private void updateState(double spd) {
            DriveState prevState = driveState;
			
            DriveState newState = prevState;
			
			switch (prevState) {
				case STOPPED:
					if (spd != DriveObject.StopSpeed){
						newState = DriveState.MOVING;
					}
					break;
				case MOVING:
					if (spd == DriveObject.StopSpeed && mDesiredFloor.getFloor() == currentFloor){
						newState = DriveState.STOPPED;
					} else if (spd == DriveObject.StopSpeed && mDesiredFloor.getFloor() != currentFloor) {
						newState = DriveState.STOPPED_UNDESIRED;
					}
					break;
				case STOPPED_UNDESIRED:
					if (spd != DriveObject.StopSpeed){
						newState = DriveState.MOVING;
					}
					break;
				default:
					break;
			}
			
			if (newState != prevState) {
				switch (newState) {
					case STOPPED:
						break;
					case MOVING:
						break;
					case STOPPED_UNDESIRED:
						message("Stopped at Undesired Floor");
						stoppedUndesiredCount++;
						break;
					default:
						break;
				}
			}
			driveState = newState;
				
        }
    }
	
	/*
     * Utility class for keeping track of the state of AtFloor.  Provides
     * external methods that can be queried to determine the floor and hallway
     * the elevator is currently at.
     *
     * Currently only updates internal state and does not call any state change
     * handler functions.
	 *
	 * @param msg
     */
    private class AtFloorStateMachine {
		
        int oldFloor = MessageDictionary.NONE;
        Hallway oldHallway = Hallway.NONE;
		
        public void receive(ReadableAtFloorPayload msg) {
            // Update for current floor
            if (msg.getFloor() == oldFloor) {
                // Additional door opening
                if (msg.getValue() == true) {
                    if ( ( oldHallway == Hallway.FRONT &&
						  msg.getHallway() == Hallway.BACK )
						|| ( oldHallway == Hallway.BACK &&
                            msg.getHallway() == Hallway.FRONT ) ) {
							oldHallway = Hallway.BOTH;
						} else {
							oldHallway = msg.getHallway();
						}
					// Leaving floor
                } else {
                    // Simple case
                    if (msg.getHallway() == oldHallway) {
                        oldHallway = Hallway.NONE;
                        oldFloor = MessageDictionary.NONE;
						// Handle weirder cases
                    } else if (oldHallway == Hallway.BOTH) {
                        if (msg.getHallway() == Hallway.FRONT) {
                            oldHallway = Hallway.BACK;
                        } else if (msg.getHallway() == Hallway.BACK) {
                            oldHallway = Hallway.FRONT;
                        } else if (msg.getHallway() == Hallway.BOTH) {
                            oldHallway = Hallway.NONE;
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
		
        public Hallway getHallway() {
            return oldHallway;
        }
    }
	
    /**
     * Utility class to detect weight changes
     */
    private class WeightStateMachine {
		
        int oldWeight = 0;
		
        public void receive(ReadableCarWeightPayload msg) {
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
		
        public void receive(ReadableDoorReversalPayload msg) {
            Hallway hall = msg.getHallway();
            Side side = msg.getSide();
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
	
	
	
	private static enum DoorState {
		
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }
	private static enum DesiredDoorState {
		
        DOOR_NOT_OPENING,
        DOOR_OPENING_DESIRED,
        DOOR_OPENING_UNDESIRED
    }
    /**
     * Utility class for keeping track of the door state.
     * 
     * Also provides external methods that can be queried to determine the
     * current door state.
     */
    private class DoorStateMachine {
		
        DoorState state[] = new DoorState[2];
		DesiredDoorState desiredDoorState;
		
		
        public DoorStateMachine() {
            state[Hallway.FRONT.ordinal()] = DoorState.CLOSED;
            state[Hallway.BACK.ordinal()] = DoorState.CLOSED;
			desiredDoorState = DesiredDoorState.DOOR_NOT_OPENING;
        }
		
        public void receive(ReadableDoorClosedPayload msg) {
            updateDoorState(msg.getHallway());
			updateDesiredDoorState(msg.getHallway());
        }
		
        public void receive(ReadableDoorOpenPayload msg) {
            updateDoorState(msg.getHallway());
			updateDesiredDoorState(msg.getHallway());
        }
		
        public void receive(ReadableDoorMotorPayload msg) {
            updateDoorState(msg.getHallway());
			updateDesiredDoorState(msg.getHallway());
        }
		
        private void updateDoorState(Hallway h) {
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
                        break;
                    case OPEN:
                        doorOpened(h);
                        break;
                    case OPENING:
                        if (previousState == DoorState.CLOSING) {
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
		
		private void updateDesiredDoorState(Hallway h) {
            DesiredDoorState prevState = desiredDoorState;
            DesiredDoorState newState = prevState;
			
			/* NEED: utility function to check that all open doors correspond
			 * to desired hallways.
			 * NEED: difference between atFloorState.getFloor and currentFloor
			 */
			switch (prevState) {
				case DOOR_NOT_OPENING:
					if (doorState.anyDoorOpen() && 
						atFloorState.getHallway()==h &&
						atFloorState.getFloor()==mDesiredFloor.getFloor() &&
						mDesiredFloor.getFloor() == currentFloor &&
						(mDesiredFloor.getHallway()==atFloorState.getHallway()||
						 mDesiredFloor.getHallway()==Hallway.BOTH)){
						
						newState = DesiredDoorState.DOOR_OPENING_DESIRED;
					}
					break;
				//case DOOR_OPENING_DESIRED:
//					if (spd == DriveObject.StopSpeed && mDesiredFloor.getFloor() == currentFloor){
//						newState = DriveState.STOPPED;
//					} else if (spd == DriveObject.StopSpeed && mDesiredFloor.getFloor() != currentFloor) {
//						newState = DriveState.STOPPED_UNDESIRED;
//					}
//					break;
//				case DOOR_OPENING_UNDESIRED:
//					if (spd != DriveObject.StopSpeed){
//						newState = DriveState.MOVING;
//					}
//					break;
				default:
					break;
			}
			
			//if (newState != prevState) {
//				switch (newState) {
//					case STOPPED:
//						break;
//					case MOVING:
//						break;
//					case STOPPED_UNDESIRED:
//						message("Stopped at Undesired Floor");
//						stoppedUndesiredCount++;
//						break;
//					default:
//						break;
//				}
//			}
			desiredDoorState = newState;
			
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
		
        public boolean allDoorsClosed(Hallway h) {
            return (doorCloseds[h.ordinal()][Side.LEFT.ordinal()].isClosed()
                    && doorCloseds[h.ordinal()][Side.RIGHT.ordinal()].isClosed());
        }
		
        public boolean allDoorMotorsStopped(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.STOP
			&& doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.STOP;
        }
		
        public boolean anyDoorMotorOpening(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.OPEN
			|| doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.OPEN;
        }
		
        public boolean anyDoorMotorClosing(Hallway h) {
            return doorMotors[h.ordinal()][Side.LEFT.ordinal()].command() == DoorCommand.CLOSE
			|| doorMotors[h.ordinal()][Side.RIGHT.ordinal()].command() == DoorCommand.CLOSE;
        }
    }
	
    /**
     * Keep track of time and decide whether to or not to include the last interval
     */
    private class ConditionalStopwatch {
		
        private boolean isRunning = false;
        private SimTime startTime = null;
        private SimTime accumulatedTime = SimTime.ZERO;
		
        /**
         * Call to start the stopwatch
         */
        public void start() {
            if (!isRunning) {
                startTime = Harness.getTime();
                isRunning = true;
            }
        }
		
        /**
         * stop the stopwatch and add the last interval to the accumulated total
         */
        public void commit() {
            if (isRunning) {
                SimTime offset = SimTime.subtract(Harness.getTime(), startTime);
                accumulatedTime = SimTime.add(accumulatedTime, offset);
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
		
        public SimTime getAccumulatedTime() {
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
        private SimTime startTime = null;
        private SimTime accumulatedTime = SimTime.ZERO;
		
        /**
         * Start the stopwatch
         */
        public void start() {
            if (!isRunning) {
                startTime = Harness.getTime();
                isRunning = true;
            }
        }
		
        /**
         * Stop the stopwatch and add the interval to the accumulated total
         */
        public void stop() {
            if (isRunning) {
                SimTime offset = SimTime.subtract(Harness.getTime(), startTime);
                accumulatedTime = SimTime.add(accumulatedTime, offset);
                startTime = null;
                isRunning = false;
            }
        }
		
        public SimTime getAccumulatedTime() {
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
