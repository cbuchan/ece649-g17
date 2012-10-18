/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan) - Author
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

/**
 * HallButtonControl controls responds to passenger input on the hall buttons
 * and eventually will notify the dispatcher when a passenger is requesting pickup
 *
 * @author Collin Buchan
 */
public class HallButtonControl extends simulator.framework.Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state
    private simulator.payloads.HallCallPayload.ReadableHallCallPayload localHallCall;
    private simulator.payloads.HallLightPayload.WriteableHallLightPayload localHallLight;

    //input network messages
    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDesiredFloor;
    private simulator.payloads.CanMailbox.ReadableCanMailbox networkAtFloor;
    private Utility.DoorClosedHallwayArray networkDoorClosed;

    //translators for input network messages
    private simulator.payloads.translators.BooleanCanPayloadTranslator mHallCall;
    private DesiredFloorCanPayloadTranslator mDesiredFloor;
    private simulator.elevatormodules.AtFloorCanPayloadTranslator mAtFloor;

    //output network messages
    private simulator.payloads.CanMailbox.WriteableCanMailbox networkHallLightOut;
    private simulator.payloads.CanMailbox.WriteableCanMailbox networkHallCallOut;

    //translators for output network messages
    private simulator.payloads.translators.BooleanCanPayloadTranslator mHallLight;

    //these variables keep track of which instance this is.
    private final simulator.framework.Hallway hallway;
    private final simulator.framework.Direction direction;
    private final int floor;

    //store the period for the controller
    private jSimPack.SimTime period;

    //enumerate states
    private enum State {
        STATE_HALL_CALL_OFF,
        STATE_HALL_CALL_ON,
    }

    //state variable initialized to the initial state FLASH_OFF
    private State state = HallButtonControl.State.STATE_HALL_CALL_OFF;

    /**
     * The arguments listed in the .cf configuration file should match the order and type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor matches the method signatures in
     * ControllerBuilder.makeAll().
     */
    public HallButtonControl(int floor, simulator.framework.Hallway hallway, simulator.framework.Direction direction, jSimPack.SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("HallButtonControl" + simulator.framework.ReplicationComputer.makeReplicationString(floor, hallway, direction), verbose);

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
        localHallCall = simulator.payloads.HallCallPayload.getReadablePayload(floor, hallway, direction);
        physicalInterface.registerTimeTriggered(localHallCall);

        //initialize physical state output
        localHallLight = simulator.payloads.HallLightPayload.getWriteablePayload(floor, hallway, direction);
        physicalInterface.sendTimeTriggered(localHallLight, period);

        //create CAN mailbox for output network messages
        networkHallLightOut = simulator.payloads.CanMailbox.getWriteableCanMailbox(MessageDictionary.HALL_LIGHT_BASE_CAN_ID +
                simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway, direction));
        networkHallCallOut = simulator.payloads.CanMailbox.getWriteableCanMailbox(
                MessageDictionary.HALL_CALL_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway, direction));

        /*
        * Create a translator with a reference to the CanMailbox.  Use the
        * translator to read and write values to the mailbox
        */
        mHallLight = new simulator.payloads.translators.BooleanCanPayloadTranslator(networkHallLightOut);
        mHallCall = new simulator.payloads.translators.BooleanCanPayloadTranslator(networkHallCallOut);

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
        networkDoorClosed = new Utility.DoorClosedHallwayArray(hallway, canInterface);
        networkDesiredFloor = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        networkAtFloor = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.AT_FLOOR_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(floor, hallway));

        mAtFloor = new simulator.elevatormodules.AtFloorCanPayloadTranslator(networkAtFloor, floor, hallway);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);

        //register to receive periodic updates to the mailbox via the CAN network
        //the period of updates will be determined by the sender of the message
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
                    newState = HallButtonControl.State.STATE_HALL_CALL_ON;
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
                if (networkDoorClosed.getAllClosed() == false && mAtFloor.getValue() == true && mDesiredFloor.getFloor() == floor &&
                        (mDesiredFloor.getDirection() == simulator.framework.Direction.STOP || mDesiredFloor.getDirection() == direction)) {
                    newState = HallButtonControl.State.STATE_HALL_CALL_OFF;
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
