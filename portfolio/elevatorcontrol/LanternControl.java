/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) - Author
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

/**
 * LanternControl controls the light within the car which shows passengers
 * which direction the car is about to travel in.
 *
 * @author Rajeev Sharma
 */
public class LanternControl extends simulator.framework.Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state
    private simulator.payloads.CarLanternPayload.WriteableCarLanternPayload localCarLantern;

    private simulator.payloads.CanMailbox.WriteableCanMailbox networkCarLanternOut;
    private simulator.payloads.translators.BooleanCanPayloadTranslator mCarLantern;

    private Utility.DoorClosedArray networkDoorClosedArray;

    private simulator.payloads.CanMailbox.ReadableCanMailbox networkDesiredFloor;
    private DesiredFloorCanPayloadTranslator mDesiredFloor;

    //Readd when used
    //private Utility.AtFloorArray networkAtFloorArray;

    //these variables keep track of which instance this is.
    private final simulator.framework.Direction direction;

    //store the period for the controller
    private jSimPack.SimTime period;

    //internal constant declarations

    //enumerate states
    private enum State {
        STATE_CAR_LANTERN_OFF,
        STATE_CAR_LANTERN_ON,
    }

    //state variable initialized to the initial state DOOR_CLOSING
    private State state = LanternControl.State.STATE_CAR_LANTERN_OFF;

    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor matches
     * the method signatures in ControllerBuilder.makeAll().
     */
    public LanternControl(simulator.framework.Direction direction, jSimPack.SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("LanternControl" + simulator.framework.ReplicationComputer.makeReplicationString(direction), verbose);

        //stored the constructor arguments in internal state
        this.period = period;
        this.direction = direction;

        log("Created LanternControl", simulator.framework.ReplicationComputer.makeReplicationString(direction));

        localCarLantern = simulator.payloads.CarLanternPayload.getWriteablePayload(direction);
        physicalInterface.sendTimeTriggered(localCarLantern, period);

        //initialize network interface
        //create a can mailbox - this object has the binary representation of the message data
        //the CAN message ids are declared in the MessageDictionary class.  The ReplicationComputer
        //class provides utility methods for computing offsets for replicated controllers
        networkCarLanternOut = simulator.payloads.CanMailbox.getWriteableCanMailbox(
                MessageDictionary.CAR_LANTERN_BASE_CAN_ID +
                        simulator.framework.ReplicationComputer.computeReplicationId(direction));
        mCarLantern = new simulator.payloads.translators.BooleanCanPayloadTranslator(networkCarLanternOut);
        canInterface.sendTimeTriggered(networkCarLanternOut, period);

        networkDoorClosedArray = new Utility.DoorClosedArray(canInterface);

        networkDesiredFloor = simulator.payloads.CanMailbox.getReadableCanMailbox(
                MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

        // Readd when used
        //networkAtFloorArray = new Utility.AtFloorArray(canInterface);

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
            case STATE_CAR_LANTERN_OFF:
                //state actions
                localCarLantern.set(false);
                mCarLantern.set(false);

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T7.1'
                //if any mDoorClosed[b,r] == false && mDesiredFloor.d == d
                if ((!networkDoorClosedArray.getAllClosed())
                        && (mDesiredFloor.getDirection() == direction)) {
                    newState = LanternControl.State.STATE_CAR_LANTERN_ON;
                } else {
                    newState = state;
                }
                break;
            case STATE_CAR_LANTERN_ON:
                //state actions
                localCarLantern.set(true);
                mCarLantern.set(true);

                //transitions
                //#transition 'T7.2'
                //if all mDoorClosed[b,r] == true || mDesiredFloor.d != d
                if ((networkDoorClosedArray.getAllClosed())
                        || (mDesiredFloor.getDirection() != direction)) {
                    newState = LanternControl.State.STATE_CAR_LANTERN_OFF;
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
