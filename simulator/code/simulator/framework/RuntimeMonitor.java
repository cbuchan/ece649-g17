/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework;

import simulator.payloads.Networkable;
import java.util.ArrayList;
import java.util.List;
import simulator.elevatorcontrol.DesiredFloorCanPayloadTranslator;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.CANNetwork.CanConnection;
import simulator.payloads.NetworkScheduler.Connection;
import simulator.payloads.AtFloorPayload;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.CanMailbox;
import simulator.payloads.CarCallPayload;
import simulator.payloads.CarCallPayload.ReadableCarCallPayload;
import simulator.payloads.CarLanternPayload;
import simulator.payloads.CarLanternPayload.ReadableCarLanternPayload;
import simulator.payloads.CarLevelPositionPayload;
import simulator.payloads.CarLevelPositionPayload.ReadableCarLevelPositionPayload;
import simulator.payloads.CarLightPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;
import simulator.payloads.CarPositionIndicatorPayload;
import simulator.payloads.CarPositionIndicatorPayload.ReadableCarPositionIndicatorPayload;
import simulator.payloads.CarWeightAlarmPayload;
import simulator.payloads.CarWeightAlarmPayload.ReadableCarWeightAlarmPayload;
import simulator.payloads.CarWeightPayload;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.DoorClosedPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorOpenPayload;
import simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload;
import simulator.payloads.DoorReversalPayload;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;
import simulator.payloads.DrivePayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DriveSpeedPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.EmergencyBrakePayload;
import simulator.payloads.EmergencyBrakePayload.ReadableEmergencyBrakePayload;
import simulator.payloads.HallCallPayload;
import simulator.payloads.HallCallPayload.ReadableHallCallPayload;
import simulator.payloads.HallLightPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;

/**
 * This class provides a basic framework for implementing runtime monitors.
 * This class is to be used for monitoring only.
 * No class that instantiates a descendent of RuntimeMonitor or contains a descendent
 * of Runtime Monitor may modify the system state in any way!  
 * That means that no controller may contain a RuntimeMonitor class.
 *
 * The RuntimeMonitor class provides event triggered access to the elevator
 * system so that you can detect physical events and record information based
 * on those events.  Physical objects are the Payload objects.  For replicated
 * objects, arrays of payload objects are created.
 * The monitor instantiates an object even for parts of the elevator that don't
 * exist (like calls and atFloors at non-existent landings) so you do not need
 * to worry about null values in the arrays and maps
 *
 * You can also receive network messages with the canInterface object
 *
 * Whenever a payload object is updated, the appropriate receive(ReadablePayload)
 * method is called.  You can override these methods to do something when the
 * event occurs.  See the documentation for Networkable for more information.
 *
 * You must implement the abstract method summarize().  This method is called
 * at the end of the acceptance test and allows you to report summary statistics.
 * In particular, you will use this when reporting performance information (Proj 7).
 *
 * If you are using the monitor for verification of high level requirements (Proj 12),
 * you will need to use the warning() method whenever a violation is detected.
 * Warnings are automatically counted and summarized at the end of the test, so
 * you do not need to include warning information in your summary() method.
 *
 * If you wish to receive timer callbacks in your monitor, be sure to use SystemTimer,
 * not the regular Timer object.  SystemTimer events execute outside the randomized
 * timing behavior of the simulator.  If you fail to do this and use a regular Timer
 * class, the elevator will behave differently depending on whether or not the
 * Monitor is instantiated.
 * 
 * @author Justin Ray
 */
public abstract class RuntimeMonitor extends Networkable implements TimeSensitive {

    final protected CanConnection canInterface = Harness.getCANNetwork().getCanConnection();
    final private Connection physicalInterface = Harness.getPhysicalNetwork().getFrameworkConnection(this);
    //data structures
    final protected ReadableDoorMotorPayload[][] doorMotors = new ReadableDoorMotorPayload[2][2];
    final protected ReadableDoorOpenPayload[][] doorOpeneds = new ReadableDoorOpenPayload[2][2];
    final protected ReadableDoorClosedPayload[][] doorCloseds = new ReadableDoorClosedPayload[2][2];
    final protected ReadableDoorReversalPayload[][] doorReversals = new ReadableDoorReversalPayload[2][2];
    final protected ReadableAtFloorPayload[][] atFloors = new ReadableAtFloorPayload[Elevator.numFloors][2];
    final protected ReadableCarCallPayload[][] carCalls = new ReadableCarCallPayload[Elevator.numFloors][2];
    final protected ReadableCarLightPayload[][] carLights = new ReadableCarLightPayload[Elevator.numFloors][2];
    final protected ReadableHallCallPayload[][][] hallCalls = new ReadableHallCallPayload[Elevator.numFloors][2][2];
    final protected ReadableHallLightPayload[][][] hallLights = new ReadableHallLightPayload[Elevator.numFloors][2][2];
    final protected ReadableCarLanternPayload[] carLanterns = new ReadableCarLanternPayload[2];
    final protected ReadableCarLevelPositionPayload carLevelPosition;
    final protected ReadableCarPositionIndicatorPayload carPositionIndicator;
    final protected ReadableCarWeightPayload carWeightPayload;
    final protected ReadableCarWeightAlarmPayload carWeightAlarmPayload;
    final protected ReadableDriveSpeedPayload driveActualSpeed;
    final protected ReadableDrivePayload driveCommandedSpeed;
    final protected ReadableEmergencyBrakePayload emergencyBrake;
    final protected DesiredFloorCanPayloadTranslator mDesiredFloor;
    final protected SystemTimer systemTimer = new SystemTimer(this);
    private int warningCount = 0;
    private String name;

    public RuntimeMonitor() {
        this.name = "RuntimeMonitor";

        //set up network inputs
        mDesiredFloor = new DesiredFloorCanPayloadTranslator(CanMailbox.getReadableCanMailbox(MessageDictionary.DESIRED_FLOOR_CAN_ID));
        canInterface.registerTimeTriggered(mDesiredFloor.getReadablePayload());
        //dummy calls to make sure the translator has the methods needed for monitoring.
        int dummyFloor = mDesiredFloor.getFloor();
        Hallway dummyHallway = mDesiredFloor.getHallway();
        Direction dummyDirection = mDesiredFloor.getDirection();


        //drive and car info (unreplicated)
        carLevelPosition = CarLevelPositionPayload.getReadablePayload();
        physicalInterface.registerEventTriggered(carLevelPosition);

        carPositionIndicator = CarPositionIndicatorPayload.getReadablePayload();
        physicalInterface.registerEventTriggered(carPositionIndicator);

        carWeightPayload = CarWeightPayload.getReadablePayload();
        physicalInterface.registerEventTriggered(carWeightPayload);

        carWeightAlarmPayload = CarWeightAlarmPayload.getReadablePayload();
        physicalInterface.registerEventTriggered(carWeightAlarmPayload);

        driveCommandedSpeed = DrivePayload.getReadablePayload();
        physicalInterface.registerEventTriggered(driveCommandedSpeed);

        driveActualSpeed = DriveSpeedPayload.getReadablePayload();
        physicalInterface.registerEventTriggered(driveActualSpeed);

        emergencyBrake = EmergencyBrakePayload.getReadablePayload();
        physicalInterface.registerEventTriggered(emergencyBrake);


        //car lanterns
        //hoistway limits
        for (Direction d : Direction.replicationValues) {
            carLanterns[d.ordinal()] = CarLanternPayload.getReadablePayload(d);
            physicalInterface.registerEventTriggered(carLanterns[d.ordinal()]);
        }


        //door values
        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                int index = ReplicationComputer.computeReplicationId(h, s);

                doorMotors[h.ordinal()][s.ordinal()] = DoorMotorPayload.getReadablePayload(h, s);
                physicalInterface.registerEventTriggered(doorMotors[h.ordinal()][s.ordinal()]);

                doorOpeneds[h.ordinal()][s.ordinal()] = DoorOpenPayload.getReadablePayload(h, s);
                physicalInterface.registerEventTriggered(doorOpeneds[h.ordinal()][s.ordinal()]);

                doorCloseds[h.ordinal()][s.ordinal()] = DoorClosedPayload.getReadablePayload(h, s);
                physicalInterface.registerEventTriggered(doorCloseds[h.ordinal()][s.ordinal()]);

                doorReversals[h.ordinal()][s.ordinal()] = DoorReversalPayload.getReadablePayload(h, s);
                physicalInterface.registerEventTriggered(doorReversals[h.ordinal()][s.ordinal()]);
            }
        }

        //landing values
        for (int i = 0; i < Elevator.numFloors; ++i) {
            for (Hallway h : Hallway.replicationValues) {
                int floor = i + 1;
                atFloors[i][h.ordinal()] = AtFloorPayload.getReadablePayload(floor, h);
                physicalInterface.registerEventTriggered(atFloors[i][h.ordinal()]);

                carCalls[i][h.ordinal()] = CarCallPayload.getReadablePayload(floor, h);
                physicalInterface.registerEventTriggered(carCalls[i][h.ordinal()]);

                carLights[i][h.ordinal()] = CarLightPayload.getReadablePayload(floor, h);
                physicalInterface.registerEventTriggered(carLights[i][h.ordinal()]);

                for (Direction d : Direction.replicationValues) {
                    hallCalls[i][h.ordinal()][d.ordinal()] = HallCallPayload.getReadablePayload(floor, h, d);
                    physicalInterface.registerEventTriggered(hallCalls[i][h.ordinal()][d.ordinal()]);

                    hallLights[i][h.ordinal()][d.ordinal()] = HallLightPayload.getReadablePayload(floor, h, d);
                    physicalInterface.registerEventTriggered(hallLights[i][h.ordinal()][d.ordinal()]);
                }
            }
        }
    }

    /**
     * Override this method to report summary statistics at the end of the acceptance
     * tests.
     *
     * @return An array of strings containing summary information.
     */
    protected abstract String[] summarize();

    /**
     * 
     * @return the number of warning() calls that have occurred.
     */
    public final int getWarningCount() {
        return warningCount;
    }

    /**
     * @return a string containing the warning stats
     */
    public final String getWarningStats() {
        return name + " generated " + warningCount + " warnings.";
    }

    public final String[] getSummaryStats() {
        String[] stats = summarize();
        String[] newArr = new String[stats.length];
        for (int i=0; i < stats.length; i++) {
            newArr[i] = name + ":  " + stats[i];
        }
        return newArr;
    }
    
    /**
     * @return name of the monitor
     */
    public final String getName() {
        return name;
    }
    
    private final void setName(String name) {
        this.name = name;
    }

    /**
     * Use this method to print informational messages to the output
     * @param message Message to be printed.
     */
    protected final void message(String message) {
        Harness.log(name, message);
    }

    /**
     * Call this method whenever a violation of requirements is detected.  Warnings
     * are automatically tracked and summarized at the end of the test.
     * @param warning A string describing the violation that occurred.
     */
    protected final void warning(String warning) {
        //System.err.format("[" + name + "] @%4.9f: WARNING:  %s", Harness.getTime().getFracSeconds(), warning);
        //System.err.println();
        Harness.log(name, "WARNING:  " + warning);
        warningCount++;
    }

    @Override
    public void receive(ReadableAtFloorPayload msg) {
        
    }
    
    @Override
    public void receive(ReadableCarCallPayload msg) {

    }

    @Override
    public void receive(ReadableCarLanternPayload msg) {

    }

    @Override
    public void receive(ReadableCarLevelPositionPayload msg) {

    }

    @Override
    public void receive(ReadableCarLightPayload msg) {

    }

    @Override
    public void receive(ReadableCarPositionIndicatorPayload msg) {

    }

    @Override
    public void receive(ReadableDoorClosedPayload msg) {

    }

    @Override
    public void receive(ReadableDoorMotorPayload msg) {

    }

    @Override
    public void receive(ReadableDoorOpenPayload msg) {

    }

    @Override
    public void receive(ReadableCarWeightPayload msg) {

    }

    @Override
    public void receive(ReadableCarWeightAlarmPayload msg) {

    }

    @Override
    public void receive(ReadableDoorReversalPayload msg) {

    }

    @Override
    public void receive(ReadableDrivePayload msg) {

    }

    @Override
    public void receive(ReadableDriveSpeedPayload msg) {

    }

    @Override
    public void receive(ReadableEmergencyBrakePayload msg) {

    }

    @Override
    public void receive(ReadableHallCallPayload msg) {

    }

    @Override
    public void receive(ReadableHallLightPayload msg) {

    }

    
    /**
     * Use reflection to create and return a monitor class of the specified name.
     * Note that the monitor must have a default (no arguments) constructor.
     * @param monitorName
     * @return
     */
    public final static RuntimeMonitor createMonitor(String monitorName) {
        ReflectionFactory rf = new ReflectionFactory();
        
        List<String> packagePath = new ArrayList<String>();
        packagePath.add("simulator.elevatorcontrol.");
        packagePath.add("simulator.elevatormodules.");
        
        RuntimeMonitor monitor = null;
        try {
            monitor = (RuntimeMonitor)rf.createObject(monitorName, packagePath);
        } catch (Exception ex) {
            throw new RuntimeException("Exception while creating runtime monitor: " + ex, ex);
        }
        return monitor;
    }
}
