/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal) - Author
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan) - Secondary Author
 * Jessica Tiu   (jtiu)
 */


package simulator.elevatorcontrol;

import jSimPack.SimTime;
import simulator.elevatorcontrol.Utility.AtFloorArray;
import simulator.elevatorcontrol.Utility.CarCallArray;
import simulator.elevatorcontrol.Utility.DoorClosedHallwayArray;
import simulator.elevatorcontrol.Utility.HallCallArray;
import simulator.elevatormodules.CarWeightCanPayloadTranslator;
import simulator.framework.Controller;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * There is one DriveControl, which controls the elevator Drive
 * (the main motor moving Car Up and Down). For simplicity we will assume
 * this node never fails, although the system could be implemented with
 * two such nodes, one per each of the Drive windings.
 *
 * @author Collin Buchan, Jesse Salazar
 */
public class Dispatcher extends Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //output network messages
    private WriteableCanMailbox networkDesiredFloor;
    private WriteableCanMailbox networkDesiredDwellFront;
    private WriteableCanMailbox networkDesiredDwellBack;


    //translators for output network messages
    private DesiredFloorCanPayloadTranslator mDesiredFloor;
    private DesiredDwellCanPayloadTranslator mDesiredDwellFront;
    private DesiredDwellCanPayloadTranslator mDesiredDwellBack;


    //input network messages
    private AtFloorArray networkAtFloorArray;
    private DoorClosedHallwayArray networkDoorClosedFront;
    private DoorClosedHallwayArray networkDoorClosedBack;
    private HallCallArray networkHallCallArray;
    private CarCallArray networkCarCallArrayFront;
    private CarCallArray networkCarCallArrayBack;
    private ReadableCanMailbox networkCarWeight;

    //translators for input network messages
    private CarWeightCanPayloadTranslator mCarWeight;

    //store the period for the controller
    private SimTime period;

    //enumerate states
    private enum State {
        STATE_INIT,
        STATE_IDLE,
        STATE_COMPUTE_NEXT,
    }

    //state variable initialized to the initial state STATE_INIT
    private int CONST_DWELL = 100;
    private State state = State.STATE_INIT;
    private int targetFloor;
    private Hallway targetHallway;

    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor matches
     * the method signatures in ControllerBuilder.makeAll().
     * <p/>
     * controllers.add(createControllerObject("DriveControl",
     * MessageDictionary.DRIVE_CONTROL_PERIOD, verbose));
     */
    public Dispatcher(SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("Dispatcher", verbose);
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
        log("Created Dispatcher with period = ", period);

        //create CAN mailbox for output network messages
        networkDesiredFloor = CanMailbox.getWriteableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        networkDesiredDwellFront = CanMailbox.getWriteableCanMailbox(
                MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(Hallway.FRONT));
        networkDesiredDwellBack = CanMailbox.getWriteableCanMailbox(
                MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(Hallway.BACK));

        /*
        * Create a translator with a reference to the CanMailbox.  Use the
        * translator to read and write values to the mailbox
        */
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        mDesiredDwellFront = new DesiredDwellCanPayloadTranslator(networkDesiredDwellFront, Hallway.FRONT);
        mDesiredDwellBack = new DesiredDwellCanPayloadTranslator(networkDesiredDwellBack, Hallway.BACK);


        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkDesiredFloor, period);
        canInterface.sendTimeTriggered(networkDesiredDwellFront, period);
        canInterface.sendTimeTriggered(networkDesiredDwellBack, period);

        /*
         * To register for network messages from the smart sensors or other objects
         * defined in elevator modules, use the translators already defined in
         * elevatormodules package.  These translators are specific to one type
         * of message.
         */
        networkAtFloorArray = new AtFloorArray(canInterface);
        networkDoorClosedFront = new DoorClosedHallwayArray(Hallway.FRONT, canInterface);
        networkDoorClosedBack = new DoorClosedHallwayArray(Hallway.BACK, canInterface);
        networkHallCallArray = new HallCallArray(canInterface);
        networkCarCallArrayFront = new CarCallArray(Hallway.FRONT, canInterface);
        networkCarCallArrayBack = new CarCallArray(Hallway.BACK, canInterface);
        networkCarWeight =
                CanMailbox.getReadableCanMailbox(MessageDictionary.CAR_WEIGHT_CAN_ID);

        mCarWeight =
                new CarWeightCanPayloadTranslator(networkCarWeight);

        //register to receive periodic updates to the mailbox via the CAN network
        //the period of updates will be determined by the sender of the message
        canInterface.registerTimeTriggered(networkCarWeight);

        /* issuing the timer start method with no callback data means a NULL value 
        * will be passed to the callback later.  Use the callback data to distinguish
        * callbacks from multiple calls to timer.start() (e.g. if you have multiple
        * timers.
        */
        timer.start(period);
    }

    /*
     * The timer callback is where the main controller code is executed.  For time
     * triggered design, this consists mainly of a switch block with a case blcok for
     * each state.  Each case block executes actions for that state, then executes
     * a transition to the next state if the transition conditions are met.
     */
    public void timerExpired(Object callbackData) {
        State newState = state;
        switch (state) {

            case STATE_INIT:

                //state actions for DRIVE_STOPPED

                //transitions
                newState = state;

                break;
            case STATE_IDLE:

                //state actions for STATE_IDLE

                //transitions
                newState = state;

                break;
            case STATE_COMPUTE_NEXT:


                //state actions for STATE_COMPUTE_NEXT

                //transitions
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

