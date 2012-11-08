/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal) - Author
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

import jSimPack.SimTime;
import simulator.elevatormodules.AtFloorCanPayloadTranslator;
import simulator.framework.*;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarCallPayload;
import simulator.payloads.CarCallPayload.ReadableCarCallPayload;
import simulator.payloads.CarLightPayload;
import simulator.payloads.CarLightPayload.WriteableCarLightPayload;
import simulator.payloads.translators.BooleanCanPayloadTranslator;
import simulator.elevatorcontrol.Utility.DoorClosedHallwayArray;


/**
 * CarButtonControl responds to passenger CarCall button presses. There is one
 * CarButtonControll per hallway[f,b]. CarButtonControl also controls CarLight
 * feedback lights.
 *
 * @author Jesse Salazar
 */
public class CarButtonControl extends Controller {

    /**
     * ************************************************************************
     * Declarations
     * ************************************************************************
     */
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical input state
    private ReadableCarCallPayload localCarCall;

    //local physical output state
    private WriteableCarLightPayload localCarLight;

    //network input interface
    private DoorClosedHallwayArray networkDoorClosedHallwayArray;
    private ReadableCanMailbox networkDesiredFloor;
    private ReadableCanMailbox networkAtFloor;

    private DesiredFloorCanPayloadTranslator mDesiredFloor;
    private AtFloorCanPayloadTranslator mAtFloor;

    //network output interface
    private WriteableCanMailbox networkCarCall;
    private WriteableCanMailbox networkCarLight;

    private BooleanCanPayloadTranslator mCarCall;
    private BooleanCanPayloadTranslator mCarLight;

    //these variables keep track of which instance this is.
    private final Hallway hallway;
    private final int floor;

    //store the period for the controller
    private SimTime period;

    //enumerate states
    private enum State {
        STATE_LIGHT_OFF,
        STATE_LIGHT_ON,
    }

    //state variable initialized to the initial state FLASH_OFF
    private State state = State.STATE_LIGHT_OFF;

    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     * <p/>
     * For your elevator controllers, you should make sure that the constructor
     * matches the method signatures in the following file:
     * simulator.framework.ControllerBuilder.makeAll()
     * <p/>
     * controllers.add(createControllerObject("CarButtonControl",floor, hallway,
     * MessageDictionary.CAR_BUTTON_CONTROL_PERIOD, verbose));
     */
    public CarButtonControl(int floor, Hallway hallway, SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("CarButtonControl" + ReplicationComputer.makeReplicationString(floor, hallway), verbose);

        //stored the constructor arguments in internal state
        this.period = period;
        this.floor = floor;
        this.hallway = hallway;

        log("Created CarButtonControl[", this.floor, "][", this.hallway, "]");

        //initialize physical input state
        localCarCall = CarCallPayload.getReadablePayload(floor, hallway);
        physicalInterface.registerTimeTriggered(localCarCall);

        //initialize physical output state
        localCarLight = CarLightPayload.getWriteablePayload(floor, hallway);
        physicalInterface.sendTimeTriggered(localCarLight, period);

        //initialize network output interface
        networkCarLight = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_LIGHT_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(floor, hallway));
        mCarLight = new BooleanCanPayloadTranslator(networkCarLight);
        canInterface.sendTimeTriggered(networkCarLight, period);

        networkCarCall = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_CALL_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(floor, hallway));
        mCarCall = new BooleanCanPayloadTranslator(networkCarCall);
        canInterface.sendTimeTriggered(networkCarCall, period);

        //initialize network input interface
        networkDoorClosedHallwayArray = new DoorClosedHallwayArray(hallway, canInterface);

        networkDesiredFloor = CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

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
            case STATE_LIGHT_OFF:
                //state actions for 'LIGHT_OFF'
                localCarLight.set(false);
                mCarLight.set(false);
                mCarCall.setValue(false);

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T9.1'
                if (localCarCall.isPressed()) {
                    newState = State.STATE_LIGHT_ON;
                } else {
                    newState = state;
                }
                break;
            case STATE_LIGHT_ON:
                //state actions for 'LIGHT_ON'
                localCarLight.set(true);
                mCarLight.set(true);
                mCarCall.setValue(true);

                //transitions -- transition conditions are mutually exclusive
                //#transition 'T9.2'
                if (mAtFloor.getValue() == true && mDesiredFloor.getFloor() == floor &&
                        (mDesiredFloor.getHallway() == hallway || mDesiredFloor.getHallway() == Hallway.BOTH)) {
                    newState = State.STATE_LIGHT_OFF;
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
