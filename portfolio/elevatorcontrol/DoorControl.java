/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) - Author
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

/**
 * DoorControl controls DoorMotor objects based on current call and safety states.
 *
 * @author Rajeev Sharma
 */
public class DoorControl extends simulator.framework.Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state
    private simulator.payloads.DoorMotorPayload.WriteableDoorMotorPayload localDoorMotor;

    private simulator.payloads.CanMailbox.WriteableCanMailbox networkDoorMotorCommandOut;
    private DoorMotorCommandCanPayloadTranslator mDoorMotorCommand;

    private Utility.AtFloorArray networkAtFloorArray;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDriveSpeed;
    private DriveSpeedCanPayloadTranslator mDriveSpeed;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDesiredFloor;
    private DesiredFloorCanPayloadTranslator mDesiredFloor;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDesiredDwell;
    private DesiredDwellCanPayloadTranslator mDesiredDwell;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDoorClosed;
    private simulator.elevatormodules.DoorClosedCanPayloadTranslator mDoorClosed;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDoorOpened;
    private simulator.elevatormodules.DoorOpenedCanPayloadTranslator mDoorOpened;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDoorReversal;
    private simulator.elevatormodules.DoorReversalCanPayloadTranslator mDoorReversal;

    private Utility.CarCallArray networkCarCallArray;

    private Utility.HallCallArray networkHallCallArray;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkCarWeight;
    private simulator.elevatormodules.CarWeightCanPayloadTranslator mCarWeight;

    //these variables keep track of which instance this is.
    private final simulator.framework.Hallway hallway;
    private final simulator.framework.Side side;

    // local state variables
    private int dwell = 0;
    private jSimPack.SimTime countDown = jSimPack.SimTime.ZERO;

    //store the period for the controller
    private jSimPack.SimTime period;

    //internal constant declarations

    //enumerate states
    private enum State {
        STATE_DOOR_CLOSING,
        STATE_DOOR_CLOSED,
        STATE_DOOR_OPENING,
        STATE_DOOR_OPEN,
        STATE_DOOR_OPEN_E,
    }

    //state variable initialized to the initial state DOOR_CLOSING
    private State state = DoorControl.State.STATE_DOOR_CLOSING;

    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor matches
     * the method signatures in ControllerBuilder.makeAll().
     */
    public DoorControl(simulator.framework.Hallway hallway, simulator.framework.Side side, jSimPack.SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("DoorControl" + simulator.framework.ReplicationComputer.makeReplicationString(hallway, side), verbose);

        //stored the constructor arguments in internal state
        this.period = period;
        this.hallway = hallway;
        this.side = side;

        log("Created DoorControl[", this.hallway, "][", this.side, "]");

        localDoorMotor = simulator.payloads.DoorMotorPayload.getWriteablePayload(hallway, side);
        physicalInterface.sendTimeTriggered(localDoorMotor, period);

        //initialize network interface
        //create a can mailbox - this object has the binary representation of the message data
        //the CAN message ids are declared in the MessageDictionary class.  The ReplicationComputer
        //class provides utility methods for computing offsets for replicated controllers
        networkDoorMotorCommandOut = simulator.payloads.CanMailbox.getWriteableCanMailbox(
                MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        mDoorMotorCommand = new DoorMotorCommandCanPayloadTranslator(
                networkDoorMotorCommandOut, hallway, side);
        canInterface.sendTimeTriggered(networkDoorMotorCommandOut, period);

        networkAtFloorArray = new Utility.AtFloorArray(canInterface);

        networkDriveSpeed = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DRIVE_SPEED_CAN_ID);
        mDriveSpeed = new DriveSpeedCanPayloadTranslator(networkDriveSpeed);
        canInterface.registerTimeTriggered(networkDriveSpeed);

        networkDesiredFloor = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

        networkDesiredDwell = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DESIRED_DWELL_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(hallway));
        mDesiredDwell = new DesiredDwellCanPayloadTranslator(
                networkDesiredDwell, hallway);
        canInterface.registerTimeTriggered(networkDesiredDwell);

        networkDoorClosed = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        mDoorClosed = new simulator.elevatormodules.DoorClosedCanPayloadTranslator(
                networkDoorClosed, hallway, side);
        canInterface.registerTimeTriggered(networkDoorClosed);

        networkDoorOpened = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DOOR_OPEN_SENSOR_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        mDoorOpened = new simulator.elevatormodules.DoorOpenedCanPayloadTranslator(
                networkDoorOpened, hallway, side);
        canInterface.registerTimeTriggered(networkDoorOpened);

        networkDoorReversal = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DOOR_REVERSAL_SENSOR_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        mDoorReversal = new simulator.elevatormodules.DoorReversalCanPayloadTranslator(
                networkDoorReversal, hallway, side);
        canInterface.registerTimeTriggered(networkDoorReversal);

        networkCarCallArray = new Utility.CarCallArray(hallway, canInterface);

        networkHallCallArray = new Utility.HallCallArray(canInterface);

        networkCarWeight = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.CAR_WEIGHT_CAN_ID);
        mCarWeight = new simulator.elevatormodules.CarWeightCanPayloadTranslator(networkCarWeight);
        canInterface.registerTimeTriggered(networkCarWeight);

        timer.start(period);
    }

    /*
     * The timer callback is where the main controller code is executed.  For time
     * triggered design, this consists mainly of a switch block with a case block for
     * each state.  Each case block executes actions for that state, then executes
     * a transition to the next state if the transition conditions are met.
     */
    @Override
    public void timerExpired(Object callbackData) {
        State newState = state;
        switch (state) {
            case STATE_DOOR_CLOSING:
                //state actions
                localDoorMotor.set(simulator.framework.DoorCommand.NUDGE);
                mDoorMotorCommand.set(simulator.framework.DoorCommand.NUDGE);

                dwell = mDesiredDwell.getDwell();
                countDown = jSimPack.SimTime.ZERO;

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T5.5'
                if (isValidHallway() && isStopped()
                        && ((isDesiredFloor() && isDesiredHallway())
                        || (isOverweight() && !doorOpened())
                        || (isDoorReversal() && !doorOpened()))) {
                    newState = DoorControl.State.STATE_DOOR_OPENING;

                }
                //#transition 'T5.1'
                else if (doorClosed()) {
                    newState = DoorControl.State.STATE_DOOR_CLOSED;
                } else {
                    newState = state;
                }
                break;
            case STATE_DOOR_CLOSED:
                //state actions
                localDoorMotor.set(simulator.framework.DoorCommand.STOP);
                mDoorMotorCommand.set(simulator.framework.DoorCommand.STOP);

                dwell = mDesiredDwell.getDwell();
                countDown = jSimPack.SimTime.ZERO;

                //transitions
                //#transition 'T5.2'
                if (isValidHallway() && isStopped()
                        && ((isDesiredFloor() && isDesiredHallway())
                        || (isOverweight() && !doorOpened())
                        || (isDoorReversal() && !doorOpened()))) {
                    newState = DoorControl.State.STATE_DOOR_OPENING;
                } else {
                    newState = state;
                }
                break;
            case STATE_DOOR_OPENING:
                //state actions
                localDoorMotor.set(simulator.framework.DoorCommand.OPEN);
                mDoorMotorCommand.set(simulator.framework.DoorCommand.OPEN);

                dwell = mDesiredDwell.getDwell();
                countDown = new jSimPack.SimTime(dwell, jSimPack.SimTime.SimTimeUnit.SECOND);

                //transitions
                //#transition 'T5.3'
                if (doorOpened() && !isOverweight() && !isDoorReversal()) {
                    newState = DoorControl.State.STATE_DOOR_OPEN;
                }
                //#transition 'T5.6'
                else if (doorOpened() && (isOverweight() || isDoorReversal())) {
                    newState = DoorControl.State.STATE_DOOR_OPEN_E;
                } else {
                    newState = state;
                }
                break;
            case STATE_DOOR_OPEN:
                //state actions
                localDoorMotor.set(simulator.framework.DoorCommand.STOP);
                mDoorMotorCommand.set(simulator.framework.DoorCommand.STOP);

                dwell = mDesiredDwell.getDwell();
                countDown = jSimPack.SimTime.subtract(countDown, period);

                //transitions
                //#transition 'T5.4'
                if (countDown.isLessThanOrEqual(jSimPack.SimTime.ZERO)) {
                    newState = DoorControl.State.STATE_DOOR_CLOSING;

                }
                //#transition 'T5.7'
                else if (isOverweight() || isDoorReversal()) {
                    newState = DoorControl.State.STATE_DOOR_OPEN_E;
                } else {
                    newState = state;
                }
                break;
            case STATE_DOOR_OPEN_E:
                //state actions
                localDoorMotor.set(simulator.framework.DoorCommand.STOP);
                mDoorMotorCommand.set(simulator.framework.DoorCommand.STOP);

                dwell = mDesiredDwell.getDwell();
                countDown = new jSimPack.SimTime(dwell, jSimPack.SimTime.SimTimeUnit.SECOND);

                //transitions
                //#transition 'T5.8'
                if (!isOverweight() && !isDoorReversal()) {
                    newState = DoorControl.State.STATE_DOOR_OPEN;
                } else {
                    newState = state;
                }
                break;
            default:
                throw new RuntimeException("State " + state + " was not recognized.");
        }

        //log the results of this iteration
        if (state == newState) {
            log("remains in state: ", state);
        } else {
            log("Transition:", state, "->", newState);
        }

        //update the state variable
        state = newState;

        //report the current state
        setState(STATE_KEY, newState.toString());

        //schedule the next iteration of the controller
        //you must do this at the end of the timer callback in order to restart
        //the timer
        timer.start(period);
    }

    private Boolean isValidHallway() {
        if (networkAtFloorArray.getCurrentFloor() == MessageDictionary.NONE) {
            return false;
        } else {
            return simulator.framework.Elevator.hasLanding(networkAtFloorArray.getCurrentFloor(), hallway);
        }
    }

    private Boolean isStopped() {
        return mDriveSpeed.getSpeed() == simulator.framework.Speed.STOP;
    }

    private Boolean isOverweight() {
        return mCarWeight.getWeight() >= simulator.framework.Elevator.MaxCarCapacity;
    }

    private Boolean doorOpened() {
        return mDoorOpened.getValue() == true;
    }

    private Boolean doorClosed() {
        return mDoorClosed.getValue() == true;
    }

    private Boolean isDoorReversal() {
        return mDoorReversal.getValue() == true;
    }

    private Boolean isDesiredFloor() {
        return networkAtFloorArray.getCurrentFloor() == mDesiredFloor.getFloor();
    }

    private Boolean isDesiredHallway() {
        return (hallway == mDesiredFloor.getHallway() || mDesiredFloor.getHallway() == simulator.framework.Hallway.BOTH);
    }
}
