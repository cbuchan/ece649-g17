package simulator.elevatormodules.passengers;

import jSimPack.SimTime;
import simulator.elevatormodules.DriveObject;
import simulator.elevatormodules.PassengerModule;
import simulator.elevatormodules.passengers.events.MotionEvent;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Speed;
import simulator.payloads.AtFloorPayload;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.CarPositionPayload;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.DrivePayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DriveSpeedPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.LevelingPayload;
import simulator.payloads.LevelingPayload.ReadableLevelingPayload;

/**
 * Monitor the drive state and provide a passenger inteface.
 *
 * This object is instance controlled since multiple instances would duplicate
 * functionality.
 *
 * @author Justin Ray
 */
class DriveMonitor extends PassengerModule {

    private static DriveMonitor instance = new DriveMonitor();

    ;

    public static DriveMonitor getInstance() {
        return instance;
    }

    public static enum DriveState {

        STOPPED,
        MOVING
    }
    //framework objects
    private ReadableAtFloorPayload[] atFloors;
    private ReadableDrivePayload driveCommand;
    private ReadableDriveSpeedPayload driveSpeed;
    private ReadableCarPositionPayload carPosition;
    private ReadableLevelingPayload[] levelSensors;
    private int currentFloor = -1;
    private DriveState driveState = DriveState.STOPPED;

    private DriveMonitor() {
        super(SimTime.ZERO, "Drive Monitor", false);
        //create and register frawework payloads
        atFloors = new ReadableAtFloorPayload[Elevator.numFloors * 2];
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                ReadableAtFloorPayload p = AtFloorPayload.getReadablePayload(floor, h);
                atFloors[ReplicationComputer.computeReplicationId(floor, h)] = p;
                physicalConnection.registerEventTriggered(p);
            }
        }
        levelSensors = new ReadableLevelingPayload[2];
        for (Direction d : Direction.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(d);
            levelSensors[index] = LevelingPayload.getReadablePayload(d);
            physicalConnection.registerEventTriggered(levelSensors[index]);
        }

        driveCommand = DrivePayload.getReadablePayload();
        physicalConnection.registerEventTriggered(driveCommand);
        driveSpeed = DriveSpeedPayload.getReadablePayload();
        physicalConnection.registerEventTriggered(driveSpeed);
        carPosition = CarPositionPayload.getReadablePayload();
        physicalConnection.registerEventTriggered(carPosition);
    }

    @Override
    public void receive(ReadableAtFloorPayload msg) {
        currentFloor = -1;
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                if (atFloors[ReplicationComputer.computeReplicationId(floor, h)].value()) {
                    currentFloor = floor;
                    return;
                }
            }
        }
    }

    @Override
    public void receive(ReadableDrivePayload msg) {
        updateDriveState();
    }

    @Override
    public void receive(ReadableDriveSpeedPayload msg) {
        updateDriveState();
    }

    @Override
    public void receive(ReadableCarPositionPayload msg) {
    }

    @Override
    public void receive(ReadableLevelingPayload msg) {
    }





    private void updateDriveState() {
        DriveState prevState = driveState;
        //consider ourselves moving if speed exceeds leveling or if we command to SLOW or FAST, otherwise we are stopped.
        if (driveSpeed.speed() > DriveObject.LevelingSpeed || driveCommand.speed() == Speed.FAST || driveCommand.speed() == Speed.SLOW) {
            driveState = DriveState.MOVING;
        } else {
            driveState = DriveState.STOPPED;
        }
        if (driveState != prevState && driveState == DriveState.MOVING) {
            firePassengerEvent(new MotionEvent(driveCommand.direction()));
        }
    }

    /**
     * 
     * @return the current floor, or -1 if car is moving or not at a floor.
     */
    public int getCurrentFloor() {
        if (driveCommand.direction() == Direction.STOP || Speed.isStopOrLevel(driveCommand.speed())) {
            return currentFloor;
        } else {
            return -1;
        }
    }

    /**
     * 
     * @return true if the car is at a floor and both level sensors are true.
     */
    public boolean isLevel() {
        return ((getCurrentFloor() != -1)
                && levelSensors[ReplicationComputer.computeReplicationId(Direction.UP)].getValue()
                && levelSensors[ReplicationComputer.computeReplicationId(Direction.DOWN)].getValue());
    }

    /**
     * 
     * @return the appropriate DriveState constant based on the car motion
     */
    public DriveState getDriveState() {
        return driveState;
    }


}
