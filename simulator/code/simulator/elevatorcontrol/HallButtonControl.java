package simulator.elevatorcontrol;

import jSimPack.SimTime;
import simulator.elevatorcontrol.Utility.DoorClosedArray;
import simulator.elevatormodules.AtFloorCanPayloadTranslator;
import simulator.elevatormodules.DoorClosedCanPayloadTranslator;
import simulator.framework.*;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.HallCallPayload;
import simulator.payloads.HallCallPayload.ReadableHallCallPayload;
import simulator.payloads.HallLightPayload;
import simulator.payloads.HallLightPayload.WriteableHallLightPayload;
import simulator.payloads.translators.BooleanCanPayloadTranslator;

/**
 * HallButtonControl controls responds to passenger input on the hall buttons
 * and eventually will notify the dispatcher when a passenger is requesting pickup
 *
 * @author Collin Buchan
 */
public class HallButtonControl extends Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state
    private ReadableHallCallPayload localHallCall;
    private WriteableHallLightPayload localHallLight;

    //input network messages
    private ReadableCanMailbox networkDoorClosedFrontLeft;
    private ReadableCanMailbox networkDesiredFloor;
    private ReadableCanMailbox networkAtFloor;
    private DoorClosedArray networkDoorClosed;

    //translators for input network messages
    private BooleanCanPayloadTranslator mHallCall;
    private DesiredFloorCanPayloadTranslator mDesiredFloor;
    private AtFloorCanPayloadTranslator mAtFloor;

    //output network messages
    private WriteableCanMailbox networkHallLightOut;
    private WriteableCanMailbox networkHallCallOut;

    //translators for output network messages
    private BooleanCanPayloadTranslator mHallLight;

    //these variables keep track of which instance this is.
    private final Hallway hallway;
    private final Direction direction;
    private final int floor;

    //store the period for the controller
    private SimTime period;

    //enumerate states
    private enum State {
        STATE_HALL_CALL_OFF,
        STATE_HALL_CALL_ON,
    }

    //state variable initialized to the initial state FLASH_OFF
    private State state = State.STATE_HALL_CALL_OFF;

    /**
     * The arguments listed in the .cf configuration file should match the order and type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor matches the method signatures in
     * ControllerBuilder.makeAll().
     */
    public HallButtonControl(int floor, Hallway hallway, Direction direction, SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("HallButtonControl" + ReplicationComputer.makeReplicationString(floor, hallway, direction), verbose);

        //stored the constructor arguments in internal state
        this.period = period;
        this.floor = floor;
        this.hallway = hallway;
        this.direction = direction;

        /*
        * The log() method is inherited from the Controller class.  It takes an
        * array of objects which will be converted to strings and concatenated
        * only if the log message is actually written.
        *
        * For performance reasons, call with comma-separated lists, e.g.:
        *   log("object=",object);
        * Do NOT call with concatenated objects like:
        *   log("object=" + object);
        */
        log("Created HallButtonControl[", this.floor, "][", this.hallway, "][", this.direction, "]");

        //initialize physical state input
        localHallCall = HallCallPayload.getReadablePayload(floor, hallway, direction);
        physicalInterface.registerTimeTriggered(localHallCall);

        //initialize physical state output
        localHallLight = HallLightPayload.getWriteablePayload(floor, hallway, direction);
        physicalInterface.sendTimeTriggered(localHallLight, period);

        //create CAN mailbox for output network messages
        networkHallLightOut = CanMailbox.getWriteableCanMailbox(MessageDictionary.HALL_LIGHT_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(floor, hallway, direction));
        networkHallCallOut = CanMailbox.getWriteableCanMailbox(
                MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, direction));

        /*
        * Create a translator with a reference to the CanMailbox.  Use the
        * translator to read and write values to the mailbox
        */
        mHallLight = new BooleanCanPayloadTranslator(networkHallLightOut);
        mHallCall = new BooleanCanPayloadTranslator(networkHallCallOut);

        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkHallLightOut, period);
        canInterface.sendTimeTriggered(networkHallCallOut, period);

        /*
         * To register for network messages from the smart sensors or other objects
         * defined in elevator modules, use the translators already defined in
         * elevatormodules package.  These translators are specific to one type
         * of message.
         */
        networkDoorClosed = new Utility.DoorClosedArray(hallway, canInterface);
        networkDesiredFloor = CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        networkAtFloor = CanMailbox.getReadableCanMailbox(
                MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));

        mAtFloor = new AtFloorCanPayloadTranslator(networkAtFloor, floor, hallway);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);

        //register to receive periodic updates to the mailbox via the CAN network
        //the period of updates will be determined by the sender of the message
        canInterface.registerTimeTriggered(networkDoorClosedFrontLeft);
        canInterface.registerTimeTriggered(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkAtFloor);

        /* issuing the timer start method with no callback data means a NULL value
         * will be passed to the callback later.  Use the callback data to distinguish
         * callbacks from multiple calls to timer.start() (e.g. if you have multiple
         * timers.
         */
        timer.start(period);
    }

    /*
     * The timer callback is where the main controller code is executed.  For time
     * triggered design, this consists mainly of a switch block with a case block for
     * each state.  Each case block executes actions for that state, then executes
     * a transition to the next state if the transition conditions are met.
     */
    public void timerExpired(Object callbackData) {
        State newState = state;
        switch (state) {
            case STATE_HALL_CALL_OFF:
                //state actions for 'HALL_CALL_OFF'
                localHallLight.set(false);
                mHallLight.set(false);
                mHallCall.set(false);

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T8.1'
                //if (localHallCall.pressed() && mDoorClosedFrontLeft.getValue() == true) {
                if (localHallCall.pressed()) {
                    newState = State.STATE_HALL_CALL_ON;
                } else {
                    newState = state;
                }
                break;
            case STATE_HALL_CALL_ON:
                //state actions for 'HALL_CALL_ON'
                localHallLight.set(true);
                mHallLight.set(true);
                mHallCall.set(true);

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T8.2'
                if (networkDoorClosed.getBothClosed() == false && mAtFloor.getValue() == true && mDesiredFloor.getFloor() == floor &&
                        (mDesiredFloor.getDirection() == Direction.STOP || mDesiredFloor.getDirection() == direction)) {
                    newState = State.STATE_HALL_CALL_OFF;
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
}
