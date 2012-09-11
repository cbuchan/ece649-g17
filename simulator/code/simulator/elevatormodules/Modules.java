package simulator.elevatormodules;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.elevatormodules.passengers.PassengerControl;
import simulator.framework.*;

/**
 * A static class that instantiates all controllers, sensors, and actuators
 * that are necessary for the elevator simulation.
 *
 * @param numFloors Number of floors in the simulation.
 * @param motorVerbose  Tell the doors to operate in verbose mode or not.
 * @param networkVerbose Tell the doors to operate in verbose mode or not.
 * @param driveVerbose Tell the doors to operate in verbose mode or not.
 * @param weightVerbose
 * Tell the weight sensor to operate in verbose mode or not.
 */
public class Modules {

    /* Sensor and actuator periods:
     */
    public final static SimTime AT_FLOOR_PERIOD = new SimTime(50000, SimTimeUnit.MICROSECOND);
    public final static SimTime WEIGHT_PERIOD = new SimTime(200000, SimTimeUnit.MICROSECOND);
    public final static SimTime CAR_LEVEL_POSITION_PERIOD = new SimTime(50000, SimTimeUnit.MICROSECOND);
    public final static SimTime DOOR_OPENED_SENSOR_PERIOD = new SimTime(200000, SimTimeUnit.MICROSECOND);
    public final static SimTime DOOR_CLOSED_SENSOR_PERIOD = new SimTime(100000, SimTimeUnit.MICROSECOND);
    public final static SimTime DOOR_REVERSAL_NETWORK_PERIOD = new SimTime(10000, SimTimeUnit.MICROSECOND);
    public final static SimTime DRIVE_PERIOD = new SimTime(10000, SimTimeUnit.MICROSECOND);
    public final static SimTime LEVEL_SENSOR_PERIOD = new SimTime(10000, SimTimeUnit.MICROSECOND);
    public final static SimTime HOISTWAY_LIMIT_PERIOD = new SimTime(200000, SimTimeUnit.MICROSECOND);
    /**** DO NOT WRITE BELOW THIS LINE.  Yet :) ****/
    public final static double HOISTWAY_LIMIT_DISTANCE = 1.0;

    /**
     * This class is a static factory, so it has no constructor visible
     * outside this class.
     */
    private Modules() {
    }

    public static PassengerControl makeAll(boolean verbose) {

        int numFloors = Elevator.numFloors;

        new Safety();

        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                new DoorOpenedSensor(DOOR_OPENED_SENSOR_PERIOD, h, s);
                new DoorClosedSensor(DOOR_CLOSED_SENSOR_PERIOD, h, s);
                new DoorReversalSensor(DOOR_REVERSAL_NETWORK_PERIOD, h, s);
            }
        }

        new CarLevelPositionSensor(CAR_LEVEL_POSITION_PERIOD,
                verbose);

        new DriveObject(DRIVE_PERIOD, verbose);

        // lay out AtFloor sensors
        for (int floor = 1; floor <= numFloors; floor++) {
            for (Hallway hallway : Hallway.replicationValues) {
                if (Elevator.hasLanding(floor, hallway)) {
                    // front hall at every floor except floor 2
                    new AtFloorSensor(AT_FLOOR_PERIOD, floor,
                            hallway);
                }
            }
        }

        new HoistwayLimitSensor(HOISTWAY_LIMIT_PERIOD,
                Direction.DOWN, -HOISTWAY_LIMIT_DISTANCE);

        new HoistwayLimitSensor(HOISTWAY_LIMIT_PERIOD,
                Direction.UP,
                Elevator.DISTANCE_BETWEEN_FLOORS * (numFloors - 1) + HOISTWAY_LIMIT_DISTANCE);

        CarWeightSensor cws = new CarWeightSensor(WEIGHT_PERIOD, verbose);

        for (Direction d : Direction.replicationValues) {
            new LevelingSensor(d, verbose);
        }

        //instantiate passenger interface objects
        HallButtonLight hallButtons[] = new HallButtonLight[Elevator.numFloors * 2 * 2];
        CarButtonLight carButtons[] = new CarButtonLight[Elevator.numFloors * 2];
        for (int floor = 1; floor <= numFloors; floor++) {
            for (Hallway hallway : Hallway.replicationValues) {
                if (Elevator.hasLanding(floor, hallway)) {
                    carButtons[ReplicationComputer.computeReplicationId(floor, hallway)] = new CarButtonLight(floor, hallway, verbose);
                    if (floor != 1) {
                        hallButtons[ReplicationComputer.computeReplicationId(floor, hallway, Direction.DOWN)] = new HallButtonLight(floor, hallway, Direction.DOWN, verbose);
                    }
                    if (floor != Elevator.numFloors) {
                        hallButtons[ReplicationComputer.computeReplicationId(floor, hallway, Direction.UP)] = new HallButtonLight(floor, hallway, Direction.UP, verbose);
                    }
                }
            }
        }

        CarLantern carLanterns = new CarLantern(verbose);

        Door[] doors = new Door[2];
        for (Hallway h : Hallway.replicationValues) {
            doors[ReplicationComputer.computeReplicationId(h)] = new Door(h, verbose);
        }

        CarWeightAlarm cwa = new CarWeightAlarm(verbose);
        CarPositionIndicator cpi = new CarPositionIndicator(verbose);

        return new PassengerControl(hallButtons, carButtons, carLanterns, doors, cpi, cwa, cws);

    }
}
