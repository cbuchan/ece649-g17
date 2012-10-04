/* 18649 Fall 2012
 * Collin Buchan
 * cbuchan
 */

package simulator.elevatorcontrol;

import jSimPack.SimTime;
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

    //network interface
    // receive hall call from the other button
    private WriteableCanMailbox networkHallLightOut;
    // translator for the hall light message -- this is a generic translator
    private BooleanCanPayloadTranslator mHallLight;

    // send network hall call
    private WriteableCanMailbox networkHallCallOut;
    // translator for the hall call message -- this is a generic translator
    private BooleanCanPayloadTranslator mHallCall;

    //received door closed message
    private ReadableCanMailbox networkDoorClosedFrontLeft;
    //translator for the doorClosed message -- this translator is specific
    //to this messages, and is provided the elevatormodules package
    private DoorClosedCanPayloadTranslator mDoorClosedFrontLeft;

    //received desired floor message
    private ReadableCanMailbox networkDesiredFloor;
    //translator for the doorClosed message -- this translator is specific
    //to this messages, and is provided the elevatormodules package
    private DesiredFloorCanPayloadTranslator mDesiredFloor;

    //received desired floor message
    private ReadableCanMailbox networkAtFloor;
    //translator for the doorClosed message -- this translator is specific
    //to this messages, and is provided the elevatormodules package
    private AtFloorCanPayloadTranslator mAtFloor;

    //these variables keep track of which instance this is.
    private final Hallway hallway;
    private final Direction direction;
    private final int floor;

    //store the period for the controller
    private SimTime period;

    //internal constant declarations

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

        //initialize physical state
        //create a payload object for this floor,hallway,direction using the
        //static factory method in HallCallPayload.
        localHallCall = HallCallPayload.getReadablePayload(floor, hallway, direction);
        //register the payload with the physical interface (as in input) -- it will be updated
        //periodically when the hall call button state is modified.
        physicalInterface.registerTimeTriggered(localHallCall);

        //create a payload object for this floor,hallway,direction
        //this is an output, so it is created with the Writeable static factory method
        localHallLight = HallLightPayload.getWriteablePayload(floor, hallway, direction);
        //register the payload to be sent periodically -- whatever value is stored
        //in the localHallLight object will be sent out periodically with the period
        //specified by the period parameter.
        physicalInterface.sendTimeTriggered(localHallLight, period);

        //initialize network interface
        //create a can mailbox - this object has the binary representation of the message data
        //the CAN message ids are declared in the MessageDictionary class.  The ReplicationComputer
        //class provides utility methods for computing offsets for replicated controllers
        networkHallLightOut = CanMailbox.getWriteableCanMailbox(MessageDictionary.HALL_LIGHT_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(floor, hallway, direction));
        /*
         * Create a translator with a reference to the CanMailbox.  Use the
         * translator to read and write values to the mailbox
         *
         * Note the use of the BooleanCanPayloadTranslator.  This translator, along with
         * IntegerCanPayloadTranslator, are provided for your use.  They are not
         * very bandwidth efficient, but they will be adequate for the first part
         * of the course.  When we get to network scheduling, you may wish to write
         * your own translators, although you can do so at any time.
         */
        mHallLight = new BooleanCanPayloadTranslator(networkHallLightOut);
        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkHallLightOut, period);


        // Registration of mHallCall message
        networkHallCallOut = CanMailbox.getWriteableCanMailbox(
                MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, direction));
        mHallCall = new BooleanCanPayloadTranslator(networkHallCallOut);
        canInterface.sendTimeTriggered(networkHallCallOut, period);

        /*
         * Registration for the mDoorClosed message is similar to the mHallLight message
         *
         * To register for network messages from the smart sensors or other objects
         * defined in elevator modules, use the translators already defined in
         * elevatormodules package.  These translators are specific to one type
         * of message.
         */
        networkDoorClosedFrontLeft = CanMailbox.getReadableCanMailbox(MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT));
        mDoorClosedFrontLeft = new DoorClosedCanPayloadTranslator(networkDoorClosedFrontLeft, Hallway.FRONT, Side.LEFT);
        //register to receive periodic updates to the mailbox via the CAN network
        //the period of updates will be determined by the sender of the message
        canInterface.registerTimeTriggered(networkDoorClosedFrontLeft);


        // Registration for the mDesiredFloor message
        networkDesiredFloor = CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

        // Registration for the mAtFloor message
        networkAtFloor = CanMailbox.getReadableCanMailbox(
                MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));
        mAtFloor = new AtFloorCanPayloadTranslator(networkAtFloor, floor, hallway);
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
                if (mDoorClosedFrontLeft.getValue() == false && mAtFloor.getValue() == true && mDesiredFloor.getFloor() == floor &&
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
