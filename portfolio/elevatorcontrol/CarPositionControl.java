/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal) - Editor
 * Rajeev Sharma (rdsharma) - Author 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;


/**
 * CarPositionControl displays the current floor of the elevator by actuating CarPositionIndicator
 *
 * @author Collin Buchan
 */
public class CarPositionControl extends simulator.framework.Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state (local output)
    private simulator.payloads.CarPositionIndicatorPayload.WriteableCarPositionIndicatorPayload localCarPositionIndicator;

    //output network messages
    private simulator.payloads.CanMailbox.WriteableCanMailbox networkCarPositionIndicator;
    private simulator.payloads.translators.IntegerCanPayloadTranslator mCarPositionIndicator;

    //input network messages
    private Utility.AtFloorArray networkAtFloorArray;
    private simulator.payloads.CanMailbox.ReadableCanMailbox networkCarLevelPosition;
    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDesiredFloor;
    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDriveSpeed;

    private simulator.elevatormodules.CarLevelPositionCanPayloadTranslator mCarLevelPosition;
    private DesiredFloorCanPayloadTranslator mDesiredFloor;
    private DriveSpeedCanPayloadTranslator mDriveSpeed;

    //store the period for the controller
    private jSimPack.SimTime period;

    //enumerate states
    private enum State {
        STATE_INIT,
        STATE_DISPLAY_FLOOR
    }

    //state variable initialized to the initial state STATE_DISPLAY_FLOOR
    private State state = CarPositionControl.State.STATE_DISPLAY_FLOOR;
    private int currentFloor; // initialized to first floor (lobby)
    private Boolean[] commitPoint;

    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor
     * matches the method signatures in the following file:
     * simulator.framework.ControllerBuilder.makeAll()
     * <p/>
     * controllers.add(createControllerObject("CarPositionControl",floor, hallway,
     * MessageDictionary.CAR_BUTTON_CONTROL_PERIOD, verbose));
     */
    public CarPositionControl(jSimPack.SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("CarPositionControl", verbose);

        //stored the constructor arguments in internal state
        this.period = period;

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
        log("Created CarPositionControl");

        //initialize physical state
        localCarPositionIndicator = simulator.payloads.CarPositionIndicatorPayload.getWriteablePayload();
        physicalInterface.sendTimeTriggered(localCarPositionIndicator, period);

        //initialize input network interface
        networkAtFloorArray = new Utility.AtFloorArray(canInterface);

        networkCarLevelPosition = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.CAR_LEVEL_POSITION_CAN_ID);
        mCarLevelPosition = new simulator.elevatormodules.CarLevelPositionCanPayloadTranslator(networkCarLevelPosition);
        canInterface.registerTimeTriggered(networkCarLevelPosition);

        networkDesiredFloor = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

        networkDriveSpeed = simulator.payloads.CanMailbox.getReadableCanMailbox(MessageDictionary.DRIVE_SPEED_CAN_ID);
        mDriveSpeed = new DriveSpeedCanPayloadTranslator(networkDriveSpeed);
        canInterface.registerTimeTriggered(networkDriveSpeed);

        //initialize output network interface
        networkCarPositionIndicator = simulator.payloads.CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_POSITION_CAN_ID);
        mCarPositionIndicator = new simulator.payloads.translators.IntegerCanPayloadTranslator(networkCarPositionIndicator);
        canInterface.sendTimeTriggered(networkCarPositionIndicator, period);

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
            case STATE_DISPLAY_FLOOR:
                //state actions for 'DISPLAY_FLOOR'
                currentFloor = networkAtFloorArray.getCurrentFloor();

                if (currentFloor != MessageDictionary.NONE) {
                    mCarPositionIndicator.set(currentFloor);
                    localCarPositionIndicator.set(currentFloor);
                }

                //transitions -- transition conditions are mutually exclusive
                newState = state;
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
