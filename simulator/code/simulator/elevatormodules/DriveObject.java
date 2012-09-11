package simulator.elevatormodules;

import jSimPack.SimTime;
import java.util.Random;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CarPositionPayload.WriteableCarPositionPayload;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DriveSpeedPayload.WriteableDriveSpeedPayload;

/**
 * Models the motion of the car in the simulation.  Motion of the car follows
 * a somewhat complex velocity profile, and responds to DrivePayload Framework
 * messages.  The position of the car is relayed to the environment via
 * CarPosition messages that are sent at the specified periodicity.
 *
 * Complex information about the drive is available by setting verbose to true
 * (mostly for debuug purposes), as well as setting verboseMode to true.
 *
 * @author jdevale
 */
public class DriveObject extends Module implements TimeSensitive {

    private final ReadableDrivePayload driveOrderedState;
    private final CarWeightPayload.ReadableCarWeightPayload carWeight;
    private int prevCarWeight = 0;
    private final WriteableDriveSpeedPayload driveSpeedState;
    //private SimTime period; = new SimTime(100000, SimTimeUnit.MICROSECOND);  //cycle time in microseconds
    private final WriteableCarPositionPayload carPositionState;

    // New message for 549 simulator
    //private double previousSpeed;
    //private Direction previousDir;
    /**
     * The maximum rate at which the drive can accelerate the car, in meters
     * per second.
     */
    public final static double Acceleration = 1.0;
    /**
     * The maximum rate at which the drive can reduce the speed of the car, in
     * meters per second.
     */
    public final static double Deceleration = 1.0;
    /**
     * The speed, in meters per second, of the car when it is stopped.
     */
    public final static double StopSpeed = 0.0;
    /**
     * The maximum speed, in meters per second, the car can travel while still
     * traveling "slowly."
     */
    public final static double SlowSpeed = 0.25;
    /**
     * The speed of the car for leveling operations
     */
    public final static double LevelingSpeed = 0.05;
    /**
     * The maximum speed, in meters per second, at which the drive can propel
     * the car.
     */
    public final static double FastSpeed = Elevator.getFastElevatorSpeed();
    private final Timer timer;
    SimTime lastRunTime;
    private final Random randomSource;
    private final static double DROP_PROBABILITY = 0.1;

    DriveObject(SimTime period, boolean verbose) {
        super(period, "Drive", verbose);

        randomSource = new Random(Harness.getRandomSeed());

        driveOrderedState = DrivePayload.getReadablePayload();
        driveSpeedState = DriveSpeedPayload.getWriteablePayload();
        carPositionState = CarPositionPayload.getWriteablePayload();
        carWeight = CarWeightPayload.getReadablePayload();
        //carLevelPosState = CarLevelPositionPayload.getReadablePayload();
        //driveOrderedState.set(Speed.STOP, Direction.STOP);
        driveSpeedState.set(Direction.STOP, 0);
        carPositionState.set(0);

        physicalConnection.sendTimeTriggered(carPositionState, period);
        physicalConnection.sendTimeTriggered(driveSpeedState, period);
        physicalConnection.registerTimeTriggered(driveOrderedState);
        physicalConnection.registerEventTriggered(driveOrderedState);
        physicalConnection.registerEventTriggered(carWeight);
        //physicalConnection.RegisterTimeTriggered(Payload.EmergencyBrakeEvent,emergencyBrakeState);

        // Send these messages on the network as well for other controllers
        //NIC.SendTimeTriggered(carLevelPosState, periodicity);
        //NIC.SendTimeTriggered(driveSpeedState, periodicity);

        physicalConnection.sendOnce(carPositionState);

        //previousSpeed = driveSpeedState.speed;
        //previousDir = driveSpeedState.direction;

        lastRunTime = Harness.getTime();

        timer = new Timer(this);
        timer.start(period);
    }

    @Override
    public void receive(ReadableDrivePayload msg) {
        run();
    }

    @Override
    public void receive(ReadableCarWeightPayload msg) {
        //see if the car weight changed
        int newWeight = msg.weight();
        if (newWeight != prevCarWeight) {
            //weight changed, so see if the car is over 2/3 full and we are not moving
            //the weight should never change while the car is moving, but better to check.
            if (newWeight > Elevator.MaxCarCapacity*2/3 && driveSpeedState.speed() == 0 && driveSpeedState.direction() == Direction.STOP) {
                //occasionally drop the car
                if (randomSource.nextDouble() < DROP_PROBABILITY ) {
                    //drop the car so that it goes out of level
                    carPositionState.set(carPositionState.position() - 2*LevelingSensor.MAX_LEVEL_ERROR);
                    physicalConnection.sendOnce(carPositionState);
                    log("Drop car out of level");
                }
            }
            //save the new weight
            prevCarWeight = newWeight;
        } //else no change
    }



    public void timerExpired(Object callbackData) {
        run();
        timer.start(period);
    }

    /**
     * Calculates the current speed and position of the car.
     */
    private void run() {

        //if no time has elapsed, do not do anything
        if (Harness.getTime().equals(lastRunTime)) {
            return;
        }

        //double deltaV, deltaX;
        double newSpeed;
        double newPosition;
        double targetSpeed = 0;
        double currentSpeed = 0;
        double acceleration = 0;

        switch (driveOrderedState.speed()) {
            case STOP:
                targetSpeed = 0.0;
                break;
            case LEVEL:
                targetSpeed = LevelingSpeed;
                break;
            case SLOW:
                targetSpeed = SlowSpeed;
                break;
            case FAST:
                targetSpeed = FastSpeed;
                break;
            default:
                throw new RuntimeException("Unknown speed");
        }
        /*
         * JDR Bug fix to make the speed stop in the case where the command is
         * Direction=STOP but speed is something other than STOP.
         */
        if (driveOrderedState.direction() == Direction.STOP) {
            targetSpeed = 0.0;
        }
        if (driveOrderedState.direction() == Direction.DOWN) {
            targetSpeed *= -1;
        }

        currentSpeed = driveSpeedState.speed();
        if (driveSpeedState.direction() == Direction.DOWN) {
            currentSpeed *= -1;
        }

        if (Math.abs(targetSpeed) > Math.abs(currentSpeed)) {
            //need to accelerate
            acceleration = Acceleration;
        } else if (Math.abs(targetSpeed) < Math.abs(currentSpeed)) {
            //need to decelerate
            acceleration = -1 * Deceleration;
        } else {
            acceleration = 0;
        }
        if (currentSpeed < 0) {
            //reverse everything for negative motion (going down)
            acceleration *= -1;
        }

        //get the time offset in seconds since the last update
        double timeOffset = SimTime.subtract(Harness.getTime(), lastRunTime).getFracSeconds();
        //remember this time as the last update
        lastRunTime = Harness.getTime();

        //now update speed
        //deltav = at
        newSpeed = currentSpeed + (acceleration * timeOffset);

        //deltax= vt+ 1/2 at^2
        newPosition = carPositionState.position() +
                (currentSpeed * timeOffset) + 
                (0.5 * acceleration * timeOffset * timeOffset);
        if ((currentSpeed < targetSpeed &&
                newSpeed > targetSpeed) ||
            (currentSpeed > targetSpeed &&
                newSpeed < targetSpeed)) {
            //if deltaV causes us to exceed the target speed, set the speed to the target speed
            driveSpeedState.setSpeed(Math.abs(targetSpeed));
        } else {
            driveSpeedState.setSpeed(Math.abs(newSpeed));
        }
        //determine the direction
        if (driveSpeedState.speed() == 0) {
            driveSpeedState.setDirection(Direction.STOP);
        } else if (targetSpeed > 0) {
            driveSpeedState.setDirection(Direction.UP);
        } else if (targetSpeed < 0) {
            driveSpeedState.setDirection(Direction.DOWN);
        }
        carPositionState.set(newPosition);

        physicalConnection.sendOnce(carPositionState);
        physicalConnection.sendOnce(driveSpeedState);

        log(" Ordered State=", driveOrderedState,
                " Speed State=", driveSpeedState,
                " Car Position=", carPositionState.position(), " meters");
    }


    @Override
    public String toString() {
        return "DriveObject";
    }
}
