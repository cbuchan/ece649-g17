package simulator.elevatorcontrol;

import jSimPack.SimTime;
import simulator.elevatorcontrol.Utility.AtFloorArray;
import simulator.elevatorcontrol.Utility.DoorClosedArray;
import simulator.elevatormodules.CarWeightCanPayloadTranslator;
import simulator.elevatormodules.LevelingCanPayloadTranslator;
import simulator.framework.Controller;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Speed;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.DrivePayload;
import simulator.payloads.DrivePayload.WriteableDrivePayload;
import simulator.payloads.translators.BooleanCanPayloadTranslator;

/**
 * There is one DriveControl, which controls the elevator Drive 
 * (the main motor moving Car Up and Down). For simplicity we will assume 
 * this node never fails, although the system could be implemented with 
 * two such nodes, one per each of the Drive windings.
 *
 * @author Jessica Tiu
 */
public class DriveControl extends Controller {

    /***************************************************************************
     * Declarations
     **************************************************************************/
    //note that inputs are Readable objects, while outputs are Writeable objects

    //local physical state
	private WriteableDrivePayload localDrive;
    
    //output network messages
	private WriteableCanMailbox networkDriveOut;
	private WriteableCanMailbox networkDriveSpeedOut;
    
    //translators for output network messages
	private DriveCommandCanPayloadTranslator mDrive;
	private DriveSpeedCanPayloadTranslator mDriveSpeed;

    //input network messages
	private ReadableCanMailbox networkLevelUp;
	private ReadableCanMailbox networkLevelDown;
	private ReadableCanMailbox networkEmergencyBrake;
	private ReadableCanMailbox networkCarWeight;
	private ReadableCanMailbox networkDesiredFloor;
  private DoorClosedArray    networkDoorClosedFront;
  private DoorClosedArray		 networkDoorClosedBack;
  private AtFloorArray       networkAtFloorArray;

	//translators for input network messages
	private LevelingCanPayloadTranslator mLevelUp;
	private LevelingCanPayloadTranslator mLevelDown;
	private BooleanCanPayloadTranslator mEmergencyBrake;
	private CarWeightCanPayloadTranslator mCarWeight;
	private DesiredFloorCanPayloadTranslator mDesiredFloor;
    
    //store the period for the controller
    private SimTime period;

    //enumerate states
    private enum State {
        STATE_DRIVE_STOPPED,
        STATE_DRIVE_LEVEL,
        STATE_DRIVE_SLOW,
    }
    
    //state variable initialized to the initial state DRIVE_STOPPED
    private State state = State.STATE_DRIVE_STOPPED;
    private Direction desiredDir;

    //returns the desired direction based on current floor and desired floor by dispatcher
    private Direction getDesiredDir(){
    	int currentFloor=networkAtFloorArray.getCurrentFloor();
    	int desiredFloor=mDesiredFloor.getFloor();
    	//current floor below desired floor
    	if (currentFloor<desiredFloor){
    		return Direction.UP;
    	}
    	//current floor above desired floor
    	else if (currentFloor>desiredFloor){
    		return Direction.DOWN;
    	}
    	//current floor is desired floor  	
    	else {
    		return Direction.STOP;
    	}
    }
    
    /**
     * The arguments listed in the .cf configuration file should match the order and
     * type given here.
     *
     * For your elevator controllers, you should make sure that the constructor matches
     * the method signatures in ControllerBuilder.makeAll().
     * 
     * controllers.add(createControllerObject("DriveControl", 
     * 				   MessageDictionary.DRIVE_CONTROL_PERIOD, verbose));
     */
    public DriveControl(SimTime period, boolean verbose) {
        //call to the Controller superclass constructor is required
        super("DriveControl", verbose);        
        this.period=period;
        
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
        log("Created DriveControl with period = ", period);
        
        //create an output payload
        localDrive = DrivePayload.getWriteablePayload();
        
        //register the payload to be sent periodically
        physicalInterface.sendTimeTriggered(localDrive, period);
      
        //create CAN mailbox for output network messages
        networkDriveOut=CanMailbox.getWriteableCanMailbox(MessageDictionary.DRIVE_COMMAND_CAN_ID);
        networkDriveSpeedOut=CanMailbox.getWriteableCanMailbox(MessageDictionary.DRIVE_SPEED_CAN_ID);
        
        /*
         * Create a translator with a reference to the CanMailbox.  Use the 
         * translator to read and write values to the mailbox
         */
        mDrive=new DriveCommandCanPayloadTranslator(networkDriveOut);
        mDriveSpeed=new DriveSpeedCanPayloadTranslator(networkDriveSpeedOut);
        
        //register the mailbox to have its value broadcast on the network periodically
        //with a period specified by the period parameter.
        canInterface.sendTimeTriggered(networkDriveOut, period);
        canInterface.sendTimeTriggered(networkDriveSpeedOut, period);

        /*
         * To register for network messages from the smart sensors or other objects
         * defined in elevator modules, use the translators already defined in
         * elevatormodules package.  These translators are specific to one type
         * of message.
         */
        networkLevelUp=
        		CanMailbox.getReadableCanMailbox(MessageDictionary.LEVELING_BASE_CAN_ID + 
        		ReplicationComputer.computeReplicationId(Direction.UP));
        networkLevelDown=
        		CanMailbox.getReadableCanMailbox(MessageDictionary.LEVELING_BASE_CAN_ID + 
        		ReplicationComputer.computeReplicationId(Direction.DOWN));
        networkEmergencyBrake=
        		CanMailbox.getReadableCanMailbox(MessageDictionary.EMERGENCY_BRAKE_CAN_ID);
        networkCarWeight=
        		CanMailbox.getReadableCanMailbox(MessageDictionary.CAR_WEIGHT_CAN_ID);
        networkDesiredFloor=
        		CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID);
        networkDoorClosedFront=new Utility.DoorClosedArray(Hallway.FRONT, canInterface);
        networkDoorClosedBack=new Utility.DoorClosedArray(Hallway.BACK, canInterface);
        networkAtFloorArray=new Utility.AtFloorArray(canInterface);
        
        mLevelUp=
        		new LevelingCanPayloadTranslator(networkLevelUp, Direction.UP);
        mLevelDown=
        		new LevelingCanPayloadTranslator(networkLevelDown, Direction.DOWN);
        mEmergencyBrake=
        		new BooleanCanPayloadTranslator(networkEmergencyBrake);
        mCarWeight=
        		new CarWeightCanPayloadTranslator(networkCarWeight);
        // used to calculate desiredDir
        mDesiredFloor=
        		new DesiredFloorCanPayloadTranslator(networkDesiredFloor);

        //register to receive periodic updates to the mailbox via the CAN network
        //the period of updates will be determined by the sender of the message
        canInterface.registerTimeTriggered(networkLevelUp);
        canInterface.registerTimeTriggered(networkLevelDown);
        canInterface.registerTimeTriggered(networkEmergencyBrake);
        canInterface.registerTimeTriggered(networkCarWeight);
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
     * triggered design, this consists mainly of a switch block with a case blcok for
     * each state.  Each case block executes actions for that state, then executes
     * a transition to the next state if the transition conditions are met.
     */
    public void timerExpired(Object callbackData) {
        State newState = state;
        switch (state) {
        
            case STATE_DRIVE_STOPPED:
            	
            	//state actions for DRIVE_STOPPED
            	/*            	
            	 * Drive[s,d]=(Stop,Stop);
            	 * mDrive[s,d]=(Stop,Stop);
            	 * mDriveSpeed[s,d]=(Stop,DesiredDirection);
            	 */
							desiredDir=getDesiredDir();

            	localDrive.set(Speed.STOP, Direction.STOP);
            	mDrive.set(Speed.STOP, Direction.STOP);
            	mDriveSpeed.set(Speed.STOP, desiredDir);
            	
            	//transitions
            	/*
            	 * T6.1 DesiredDirection~=Stop && mDoorClosed[*,*]==True && 
            	 * 		mCarWeight<MaxCarCapacity && mEmergencyBrake[b]==Off
            	 * T6.5 DesiredDirection==Stop && mDoorClosed==True && 
            	 * 		mDesiredFloor.f==CurrentFloor && mLevel[d]==False (for any d)
            	 */
            	if (!desiredDir.equals(Direction.STOP) && 
            			networkDoorClosedFront.getBothClosed() && networkDoorClosedBack.getBothClosed() &&
            			mCarWeight.getWeight()<Elevator.MaxCarCapacity &&
            			!mEmergencyBrake.getValue()){            		
            		newState = State.STATE_DRIVE_SLOW;
            	}
            	else if (desiredDir.equals(Direction.STOP) &&
            			networkDoorClosedFront.getBothClosed() && networkDoorClosedBack.getBothClosed() &&
            			mDesiredFloor.getFloor()==networkAtFloorArray.getCurrentFloor() &&
            			(!mLevelUp.getValue() || !mLevelDown.getValue())){
     		
            		newState = State.STATE_DRIVE_LEVEL;
            	}
            	else { newState=state; }
            	
                break;
            case STATE_DRIVE_LEVEL:
            	
            	//state actions for DRIVE_LEVEL
            	/*            	
            	 * Drive[s,d]=(Level,DesiredDirection);
            	 * mDrive[s,d]=(Level,DesiredDirection);
            	 * mDriveSpeed[s,d]=(Stop,Stop);
            	 */
							desiredDir=getDesiredDir();

            	localDrive.set(Speed.LEVEL, desiredDir);
            	mDrive.set(Speed.LEVEL, desiredDir);
            	mDriveSpeed.set(Speed.STOP, Direction.STOP);
            	            	
            	//transitions
            	/*
            	 * T6.4 (mLevel[*]==True && mDesiredFloor.f==CurrentFloor) || mEmergencyBrake[b]==On
            	 */
            	if ((mLevelUp.getValue() || mLevelDown.getValue()) &&
            			mDesiredFloor.getFloor()==networkAtFloorArray.getCurrentFloor() ||
            			mEmergencyBrake.getValue()){            		
            		newState = State.STATE_DRIVE_STOPPED;
            		
            	} else { newState = state; }
            		
                break;
            case STATE_DRIVE_SLOW:
            	
            	//state actions for DRIVE_SLOW
            	/*            	
            	 * Drive[s,d]=(Slow,d);
            	 * mDrive[s,d]=(Slow,d);
            	 * mDriveSpeed[s,d]=(s,Stop);
            	 */

							desiredDir=getDesiredDir();

            	localDrive.set(Speed.SLOW, desiredDir);
            	mDrive.set(Speed.SLOW, desiredDir);
            	mDriveSpeed.set(Speed.SLOW, desiredDir);
            	            	
            	//transitions
            	/*
            	 * T6.2 mEmergencyBrake[b]==On
            	 * T6.3 DesiredDirection==Stop && mDoorClosed==True && 
            	 * 		mDesiredFloor.f==CurrentFloor && mLevel[d]==False (for any d)
            	 */
            	if (mEmergencyBrake.getValue()){
            		newState = State.STATE_DRIVE_STOPPED;
            	}
            	else if (desiredDir.equals(Direction.STOP) &&
            			networkDoorClosedFront.getBothClosed() && networkDoorClosedBack.getBothClosed() &&
            			mDesiredFloor.getFloor()==networkAtFloorArray.getCurrentFloor() &&
            			(!mLevelUp.getValue() || !mLevelDown.getValue())){
            		newState = State.STATE_DRIVE_LEVEL;
            	}
            	else { newState = state; }
            	
                break;
            default:
                throw new RuntimeException("State " + state + " was not recognized.");
        }
        
        //log the results of this iteration
        if (state == newState) {
            log("remains in state: ",state);
        } else {
            log("Transition:",state,"->",newState);
        }

        //update the state variable
        state = newState;

        //report the current state
        setState(STATE_KEY,newState.toString());

        //schedule the next iteration of the controller
        //you must do this at the end of the timer callback in order to restart
        //the timer
        timer.start(period);
    }
}

