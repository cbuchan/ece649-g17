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
import simulator.elevatormodules.DoorClosedCanPayloadTranslator;
import simulator.framework.*;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarCallPayload;
import simulator.payloads.CarCallPayload.ReadableCarCallPayload;
import simulator.payloads.CarLightPayload;
import simulator.payloads.CarLightPayload.WriteableCarLightPayload;
import simulator.payloads.translators.BooleanCanPayloadTranslator;


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

    //local physical state
    private ReadableCarCallPayload localCarCall;
    private WriteableCarLightPayload localCarLight;

    //network interface
    // Output mCarCall message 
    private WriteableCanMailbox networkCarCall;
    // translator for the CarCall message -- this is a generic translator
    private BooleanCanPayloadTranslator mCarCall;
	
	//network interface
    // Output mCarLight message 
    private WriteableCanMailbox networkCarLightOut;
    // translator for the CarLight message -- this is a generic translator
    private BooleanCanPayloadTranslator mCarLight;

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
    private final int floor;

    //store the period for the controller
    private SimTime period;

    //internal constant declarations
	

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
	 *			simulator.framework.ControllerBuilder.makeAll()
	 *
	 * controllers.add(createControllerObject("CarButtonControl",floor, hallway,
     * 				   MessageDictionary.CAR_BUTTON_CONTROL_PERIOD, verbose));
     */ 
    public CarButtonControl(int floor, Hallway hallway, SimTime period, 
							 boolean verbose) {
        //call to the Controller superclass constructor is required
        super("CarButtonControl" + 
			  ReplicationComputer.makeReplicationString(floor, hallway), 
			  verbose);

        //stored the constructor arguments in internal state
        this.period = period;
        this.floor = floor;
        this.hallway = hallway;

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
        log("Created CarButtonControl[", this.floor, "][", this.hallway, "]");

        //initialize physical state
        //create a payload object for this floor,hallway using the
        //static factory method in CarCallPayload.
        localCarCall = CarCallPayload.getReadablePayload(floor, hallway);
        //register the payload with the physical interface (as in input) 
		// => It will be updated periodically when the car call button 
		// state is modified.
        physicalInterface.registerTimeTriggered(localCarCall);

        //create a payload object for this floor,hallway
        //this is an output, so it is created with the Writeable static factory method
        localCarLight = CarLightPayload.getWriteablePayload(floor, hallway);
        //register the payload to be sent periodically -- whatever value is stored
        //in the localCarLight object will be sent out periodically with the period
        //specified by the period parameter.
        physicalInterface.sendTimeTriggered(localCarLight, period);

		
		
		
		
        //initialize network interface
        //create a can mailbox - this object has the binary representation of the message data
        //the CAN message ids are declared in the MessageDictionary class.  The ReplicationComputer
        //class provides utility methods for computing offsets for replicated controllers
        networkCarLightOut = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_LIGHT_BASE_CAN_ID +
                ReplicationComputer.computeReplicationId(floor, hallway));
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
        mCarLight = new BooleanCanPayloadTranslator(networkCarLightOut);
        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkCarLightOut, period);
		
		
		
		
		//initialize network interface
        //create a can mailbox - this object has the binary representation of the message data
        //the CAN message ids are declared in the MessageDictionary class.  The ReplicationComputer
        //class provides utility methods for computing offsets for replicated controllers
        networkCarCall = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_CALL_BASE_CAN_ID +
															   ReplicationComputer.computeReplicationId(floor, hallway));
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
        mCarCall = new BooleanCanPayloadTranslator(networkCarCall);
        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkCarCall, period);
		
		
		
        /*
         * Registration for the mDoorClosed message is similar to the mCarLight message
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

		
		
        /*
        * Registration for the mDesiredFloor message
        */
        networkDesiredFloor = CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(networkDesiredFloor);
        canInterface.registerTimeTriggered(networkDesiredFloor);

		
        /*
        * Registration for the mAtFloor message
        */
        networkAtFloor = CanMailbox.getReadableCanMailbox(
                MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));
        mAtFloor = new AtFloorCanPayloadTranslator(networkAtFloor, floor, hallway);
        canInterface.registerTimeTriggered(networkDesiredFloor);

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
            case STATE_LIGHT_ON:
                //state actions for 'LIGHT_ON'
				mCarCall.setValue(localCarCall.isPressed()); 
                localCarLight.set(true);
                mCarLight.set(localCarLight.isLighted());

                //transitions -- transition conditions are mutually exclusive
                //#transition 'T9.1'
                if (mCarCall.getValue()==false) {
                    newState = State.STATE_LIGHT_OFF;
                } else {
                    newState = state;
                }
                break;
            case STATE_LIGHT_OFF:
                //state actions for 'LIGHT_OFF'
                mCarCall.setValue(localCarCall.isPressed()); 
                localCarLight.set(false);
                mCarLight.set(localCarLight.isLighted());

                //transitions -- note that transition conditions are mutually exclusive
                //#transition 'T9.2'
                if (mCarCall.getValue()==true) {
                    newState = State.STATE_LIGHT_ON;
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
