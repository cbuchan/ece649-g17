package simulator.elevatormodules;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.payloads.*;
import simulator.framework.*;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.AtFloorPayload.WriteableAtFloorPayload;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.CarWeightPayload.WriteableCarWeightPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorClosedPayload.WriteableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorMotorPayload.WriteableDoorMotorPayload;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;
import simulator.payloads.DoorReversalPayload.WriteableDoorReversalPayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DrivePayload.WriteableDrivePayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.DriveSpeedPayload.WriteableDriveSpeedPayload;
import simulator.payloads.EmergencyBrakePayload.WriteableEmergencyBrakePayload;
import simulator.payloads.HoistwayLimitPayload.ReadableHoistwayLimitPayload;
import simulator.payloads.HoistwayLimitPayload.WriteableHoistwayLimitPayload;
import static simulator.elevatormodules.DriveObject.*;

/**
 * A safety certification node that looks for "dangerous" (as specified in the
 * elevator requirements) operation of the elevator and sends an
 * <code>EmergencyBrakePayload</code> message on the framework and network if
 * such a condition is detected.  A SafetyViolationException is also thrown
 * which ends the simulation.
 */
public class Safety extends Module implements TimeSensitive {

    private static final SimTime DOOR_REVERSAL_PERIOD = new SimTime(200, SimTimeUnit.MILLISECOND);
    Timer[] reversalTimer;
    Object[] reversalTimerCallback;
    ReadableHoistwayLimitPayload[] hoistwayLimits;
    ReadableAtFloorPayload[] atFloors;
    ReadableDoorMotorPayload[] doorMotors;
    ReadableDoorClosedPayload[] doorCloseds;
    ReadableDoorReversalPayload[] doorReversals;
    ReadableDrivePayload driveCommand;
    ReadableDriveSpeedPayload driveSpeed;
    ReadableCarWeightPayload carWeight;
    WriteableEmergencyBrakePayload ebrake;
    SafetySensorCanPayloadTranslator mSafety;

    public Safety() {
        super(new SimTime(50, SimTimeUnit.MILLISECOND), "Safety", true);

        //initialize payloads
        hoistwayLimits = new ReadableHoistwayLimitPayload[2];
        for (Direction d : Direction.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(d);
            hoistwayLimits[index] = HoistwayLimitPayload.getReadablePayload(d);
            physicalConnection.registerEventTriggered(hoistwayLimits[index]);
        }

        atFloors = new ReadableAtFloorPayload[Elevator.numFloors * 2];
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                int index = ReplicationComputer.computeReplicationId(floor, h);
                atFloors[index] = AtFloorPayload.getReadablePayload(floor, h);
                physicalConnection.registerEventTriggered(atFloors[index]);
            }
        }

        doorCloseds = new ReadableDoorClosedPayload[4];
        doorMotors = new ReadableDoorMotorPayload[4];
        doorReversals = new ReadableDoorReversalPayload[4];
        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                int index = ReplicationComputer.computeReplicationId(h, s);
                doorMotors[index] = DoorMotorPayload.getReadablePayload(h, s);
                physicalConnection.registerEventTriggered(doorMotors[index]);
                doorCloseds[index] = DoorClosedPayload.getReadablePayload(h, s);
                physicalConnection.registerEventTriggered(doorCloseds[index]);
                doorReversals[index] = DoorReversalPayload.getReadablePayload(h, s);
                physicalConnection.registerEventTriggered(doorReversals[index]);
            }
        }

        driveCommand = DrivePayload.getReadablePayload();
        physicalConnection.registerEventTriggered(driveCommand);

        driveSpeed = DriveSpeedPayload.getReadablePayload();
        physicalConnection.registerEventTriggered(driveSpeed);

        carWeight = CarWeightPayload.getReadablePayload();
        physicalConnection.registerEventTriggered(carWeight);

        ebrake = EmergencyBrakePayload.getWriteablePayload();
        ebrake.set(false);
        physicalConnection.sendOnce(ebrake);

        WriteableCanMailbox wcm = CanMailbox.getWriteableCanMailbox(MessageDictionary.EMERGENCY_BRAKE_CAN_ID);
        mSafety = new SafetySensorCanPayloadTranslator(wcm);
        mSafety.setValue(false);
        canNetworkConnection.sendTimeTriggered(wcm, period);

        //initialize timers
        reversalTimer = new Timer[2];
        reversalTimerCallback = new Object[2];
        for (Hallway h : Hallway.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(h);
            reversalTimer[index] = new Timer(this);
            reversalTimerCallback[index] = new Object();
        }
    }

    //check methods - these methods do the actual condition checking
    /**
     * Timer callback checks that reversals have been responded to after the
     * reversal timers expire.
     *
     * @param callBackData  lets us distinguish front and rear reversal callbacks
     */
    public void timerExpired(Object callBackData) {
        for (Hallway h : Hallway.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(h);
            if (callBackData == reversalTimerCallback[index]) {
                if (!isDoorOpenOrNudge(h)) {
                    EngageBrake(h.toString() + " Door Reversal not canceled");
                }
                return;
            }
        }
    }

    /**
     * Engage brake if:
     *   The doors are not closed at a floor where there is no landing
     */
    private void checkDoorOpenNoLanding() {
        //check for doors opening with no landing
        int currentFloor = getCurrentFloor();
        if (currentFloor != MessageDictionary.NONE) {
            for (Hallway h : Hallway.replicationValues) {
                if (!isDoorClosed(h) && !Elevator.hasLanding(currentFloor, h)) {
                    EngageBrake("At least 1 " + h + " Door Closed is false and there is no landing at " + h + " Hallway on floor " + currentFloor);
                }
            }
        }
    }

    /**
     * Engage brake if:
     *   the car moves with the doors open (except for leveling)
     */
    private void checkDoorOpenInMotion() {
        //ebrake if the car is moving and the doors are not closed
        if ((!isDoorClosed(Hallway.FRONT) || !isDoorClosed(Hallway.BACK)) && !isDriveStopOrLevel()) {
            EngageBrake("Car in motion with the doors open.");
        }
    }

    /**
     * Check for reversal and start the appropriate reversal timer if found
     */
    private void startReversalTimers() {
        for (Hallway h : Hallway.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(h);
            if (isReversing(h) && !reversalTimer[index].isRunning() && !isDoorOpenOrNudge(h)) {
                reversalTimer[index].start(DOOR_REVERSAL_PERIOD, reversalTimerCallback[index]);
            }
        }
    }

    /**
     * Check the state of the door command and cancel the reversal timer if the doors are commanded to open or nudge
     */
    private void checkReversalCancel() {
        for (Hallway h : Hallway.replicationValues) {
            int index = ReplicationComputer.computeReplicationId(h);
            if (reversalTimer[index].isRunning() && isDoorOpenOrNudge(h)) {
                reversalTimer[index].cancel();
            }
        }
    }

    /**
     * Engage the brake if: 
     *   the car moves while overweight (except for leveling)
     *   the drive command is not adjacent to the drive speed
     */
    private void checkDrive() {

        // If car is moving when the car is overloaded, or the drive is
        // commanded to move when the car is overloaded, engage the emergency brake
        if (carWeight.weight() >= Elevator.MaxCarCapacity && !isDriveStopOrLevel()) {
            EngageBrake("Car Weight is overloaded while Drive is moving or commanded to move.");
        }

        /* Drive command is not adjacent to physical DriveSpeed */
        /* Car is moving faster than slow speed, but command is STOP */
        if ((driveSpeed.speed() > SlowSpeed && (Speed.isStopOrLevel(driveCommand.speed()) || driveCommand.direction() == Direction.STOP))) {
            EngageBrake("Drive command not adjacent to DriveSpeed: Commanded to " + driveCommand.speed() + "," + driveCommand.direction() + "while moving faster than SlowSpeed");
        }
        //System.out.println("DriveSpeed: " +  driveSpeed.speed() + "," + driveSpeed.direction());
        /* Car is moving slower than slow speed, but command is FAST */
        if ((driveSpeed.speed() < SlowSpeed && driveCommand.speed() == Speed.FAST)) {
            EngageBrake("Drive command not adjacent to DriveSpeed: Commanded to " + driveCommand.speed() + "," + driveCommand.direction() + "while moving slower than SlowSpeed");
        }
        /* Car is commanded to the opposite direction while in motion */
        if (driveSpeed.direction() != driveCommand.direction() && driveSpeed.speed() > 0 && driveCommand.direction() != Direction.STOP) {
            EngageBrake("Drive command not adjacent to DriveSpeed: Commanded to " + driveCommand.speed() + "," + driveCommand.direction() + "while moving in the direction " + driveSpeed.direction());
        }
    }

    /**
     * engage the brake if the hoistway limit is exceeded.
     * @param hoistwayLimits the hoistway limit payload that was just updated (from the receive method)
     */
    private void checkHoistwayLimit(ReadableHoistwayLimitPayload hlp) {
        if (hlp.exceeded()) {
            EngageBrake("safety violation: hoistway limit switch tripped");
            // # transition "Any Hoistway Limit[d] == true"
        }
    }

    //Networkable methods - these methods trigger the check methods
    @Override
    public void receive(ReadableDoorClosedPayload mp) {
        //System.out.println("Door Closed" + ReplicationComputer.makeReplicationString(mp.getHallway(), mp.getSide()) + " = " + mp.isClosed());
        checkDoorOpenNoLanding();
        checkDoorOpenInMotion();
    }

    @Override
    public void receive(ReadableAtFloorPayload mp) {
        checkDoorOpenNoLanding();
    }

    @Override
    public void receive(ReadableDoorMotorPayload mp) {
        checkReversalCancel();
    }

    @Override
    public void receive(ReadableDoorReversalPayload mp) {
        startReversalTimers();
    }

    @Override
    public void receive(ReadableDriveSpeedPayload p) {
        checkDrive();
        checkDoorOpenInMotion();
    }

    @Override
    public void receive(ReadableDrivePayload mp) {
        checkDrive();
    }

    @Override
    public void receive(ReadableHoistwayLimitPayload mp) {
        checkHoistwayLimit(mp);
    }

    @Override
    public void receive(ReadableCarWeightPayload msg) {
        checkDrive();
    }

    //utility methods
    private void EngageBrake(String msg) {
        log("EMERGENCY BRAKE ENGAGED! " + msg);
        // BL 10/30/02 - now this is on the network as well
        //EmergencyBrakePayload sendme = EmergencyBrakePayload.getReadablePayload();
        //sendme.value = true;
        ebrake.set(true);
        mSafety.setValue(true);
        physicalConnection.sendOnce(ebrake);
        throw new SafetyViolationException("EMERGENCY BRAKE ENGAGED! " + msg);
        //Harness.endSim();
    }

    /**
     * 
     * @param hallway  which door to check
     * @return true if both doors are commanded to open or nudge
     */
    private boolean isDoorOpenOrNudge(Hallway hallway) {
        return (doorMotors[ReplicationComputer.computeReplicationId(hallway, Side.LEFT)].command() == DoorCommand.OPEN
                || doorMotors[ReplicationComputer.computeReplicationId(hallway, Side.LEFT)].command() == DoorCommand.NUDGE)
                && (doorMotors[ReplicationComputer.computeReplicationId(hallway, Side.RIGHT)].command() == DoorCommand.OPEN
                || doorMotors[ReplicationComputer.computeReplicationId(hallway, Side.RIGHT)].command() == DoorCommand.NUDGE);
    }

    /**
     * 
     * @param hallway which set of doors to check
     * @return true if both doors are closed
     */
    private boolean isDoorClosed(Hallway hallway) {
        return (doorCloseds[ReplicationComputer.computeReplicationId(hallway, Side.LEFT)].isClosed() && doorCloseds[ReplicationComputer.computeReplicationId(hallway, Side.RIGHT)].isClosed());
    }

    /**
     * 
     * @param hallway which set of doors to check
     * @return true if either door is reversing
     */
    private boolean isReversing(Hallway hallway) {
        return doorReversals[ReplicationComputer.computeReplicationId(hallway, Side.LEFT)].isReversing() || doorReversals[ReplicationComputer.computeReplicationId(hallway, Side.RIGHT)].isReversing();
    }

    /**
     * 
     * @return true if the current drive command is STOP or LEVEL and the actual
     * drive speed is less than or equal to leveling.
     */
    private boolean isDriveStopOrLevel() {
        return (driveCommand.direction() == Direction.STOP || driveCommand.speed() == Speed.STOP || driveCommand.speed() == Speed.LEVEL)
                && (driveSpeed.speed() <= DriveObject.LevelingSpeed);
    }

    /**
     * 
     * @return the current floor, or NONE if no active atFloors
     */
    private int getCurrentFloor() {

        int ret = MessageDictionary.NONE;

        //System.out.println("\n*****************************\n\n");
        for (ReadableAtFloorPayload afp : atFloors) {
            //System.out.println("AtFloor in safety: " + afp.toString());
            if (afp.value() == false) {
                continue;
            }
            if (MessageDictionary.NONE == ret) {
                ret = afp.getFloor();
            } else if (afp.getFloor() != ret) {
                throw new RuntimeException("Control: @TIME " + Harness.getTime()
                        + ": AtFloor is true for more than one floor!");
            }
        }

        return ret;
    }

    @Override
    public String toString() {
        return "Safety";
    }

    /**
     * Test module for injecting test values for testing the Safety object.  This
     * class should only be instantiated in the main() method for the Safety class.
     */
    private static class SafetyTest extends Module implements TimeSensitive {

        private WriteableHoistwayLimitPayload hoistwayLimits[] = new WriteableHoistwayLimitPayload[2];
        private WriteableCarWeightPayload carWeight;
        private WriteableAtFloorPayload[] atFloor = new WriteableAtFloorPayload[Elevator.numFloors * 2];
        private WriteableDriveSpeedPayload driveSpeed;
        private WriteableDrivePayload driveCommand;
        private WriteableDoorReversalPayload[] doorReversals = new WriteableDoorReversalPayload[4];
        private WriteableDoorMotorPayload[] doorMotors = new WriteableDoorMotorPayload[4];
        private WriteableDoorClosedPayload[] doorCloseds = new WriteableDoorClosedPayload[4];
        private Timer timer = new Timer(this);
        private boolean expectException = true;
        private SimTime REVERSAL_CANCEL_PERIOD = new SimTime(100, SimTime.SimTimeUnit.MILLISECOND);
        private SimTime TEST_PERIOD = new SimTime(25, SimTimeUnit.MILLISECOND);
        private final int whichTest;

        private SafetyTest(int whichTest) {
            super(new SimTime(50, SimTime.SimTimeUnit.MILLISECOND), "SafetyTest", true);
            this.whichTest = whichTest;
            //initialize senders
            for (Direction d : Direction.replicationValues) {
                int index = ReplicationComputer.computeReplicationId(d);
                hoistwayLimits[index] = HoistwayLimitPayload.getWriteablePayload(d);
                hoistwayLimits[index].set(false);
                physicalConnection.sendTimeTriggered(hoistwayLimits[index], TEST_PERIOD);
            }

            for (int floor = 1; floor < Elevator.numFloors; floor++) {
                for (Hallway h : Hallway.replicationValues) {
                    int index = ReplicationComputer.computeReplicationId(floor, h);
                    atFloor[index] = AtFloorPayload.getWriteablePayload(floor, h);
                    atFloor[index].set(false);
                    physicalConnection.sendTimeTriggered(atFloor[index], TEST_PERIOD);
                }
            }

            for (Hallway h : Hallway.replicationValues) {
                for (Side s : Side.values()) {
                    int index = ReplicationComputer.computeReplicationId(h, s);
                    doorReversals[index] = DoorReversalPayload.getWriteablePayload(h, s);
                    doorReversals[index].set(false);
                    physicalConnection.sendTimeTriggered(doorReversals[index], TEST_PERIOD);
                    doorMotors[index] = DoorMotorPayload.getWriteablePayload(h, s);
                    doorMotors[index].set(DoorCommand.STOP);
                    physicalConnection.sendTimeTriggered(doorMotors[index], TEST_PERIOD);
                    doorCloseds[index] = DoorClosedPayload.getWriteablePayload(h, s);
                    doorCloseds[index].set(true);
                    physicalConnection.sendTimeTriggered(doorCloseds[index], TEST_PERIOD);
                }
            }

            carWeight = CarWeightPayload.getWriteablePayload();
            carWeight.set(0);
            physicalConnection.sendTimeTriggered(carWeight, TEST_PERIOD);
            driveSpeed = DriveSpeedPayload.getWriteablePayload();
            driveSpeed.set(Direction.STOP, 0);
            physicalConnection.sendTimeTriggered(driveSpeed, TEST_PERIOD);
            driveCommand = DrivePayload.getWriteablePayload();
            driveCommand.set(Speed.STOP, Direction.STOP);
            physicalConnection.sendTimeTriggered(driveCommand, TEST_PERIOD);

            Timer t = new Timer(new TimeSensitive() {

                public void timerExpired(Object callbackData) {
                    run();
                }
            });
            t.start(new SimTime(10, SimTime.SimTimeUnit.SECOND));
        }

        public void timerExpired(Object callbackData) {
            Integer i = (Integer) callbackData;

            switch (i) {
                case 17:
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(DoorCommand.OPEN);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(DoorCommand.OPEN);
                    break;
                case 18:
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(DoorCommand.OPEN);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(DoorCommand.OPEN);
                    break;
                case 19:
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(DoorCommand.NUDGE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(DoorCommand.NUDGE);
                    break;
                case 20:
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(DoorCommand.NUDGE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(DoorCommand.NUDGE);
                    break;
                default:
                    throw new RuntimeException("Unknown index " + i);
            }

        }

        public void run() {
            switch (whichTest) {
                //see if hoistway limits trigger the ebrake
                case 0:
                    Harness.log(name, "HoistwayLimit UP test");
                    hoistwayLimits[ReplicationComputer.computeReplicationId(Direction.UP)].set(true);
                    physicalConnection.sendOnce(hoistwayLimits[ReplicationComputer.computeReplicationId(Direction.UP)]);
                    break;
                case 1:
                    Harness.log(name, "HoistwayLimit DOWN test");
                    hoistwayLimits[ReplicationComputer.computeReplicationId(Direction.DOWN)].set(true);
                    break;
                //see if owerweight movevment triggeres
                case 2:
                    Harness.log(name, "Move while overweight test");
                    atFloor[ReplicationComputer.computeReplicationId(2, Hallway.BACK)].set(true);
                    carWeight.set(Elevator.MaxCarCapacity + 100);
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    break;
                //check that leveling does not trigger
                case 3:
                    Harness.log(name, "Level while overweight test");
                    expectException = false;
                    carWeight.set(Elevator.MaxCarCapacity + 100);
                    driveSpeed.set(Direction.UP, DriveObject.LevelingSpeed);
                    break;
                case 4:
                    Harness.log(name, "Drivespeed not adjacent: FAST while not slow");
                    driveSpeed.set(Direction.UP, DriveObject.LevelingSpeed);
                    driveCommand.set(Speed.FAST, Direction.UP);
                    break;
                case 5:
                    Harness.log(name, "Drivespeed not adjacent: LEVEL while not slow");
                    driveSpeed.set(Direction.UP, DriveObject.FastSpeed);
                    driveCommand.set(Speed.LEVEL, Direction.UP);
                    break;
                case 6:
                    Harness.log(name, "Drivespeed not adjacent: STOP while not slow");
                    driveSpeed.set(Direction.UP, DriveObject.FastSpeed);
                    driveCommand.set(Speed.STOP, Direction.UP);
                    break;
                case 7:
                    Harness.log(name, "Drivespeed not adjacent: opposite direction");
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    driveCommand.set(Speed.SLOW, Direction.DOWN);
                    break;
                case 8:
                    Harness.log(name, "Drivespeed adjacent: LEVEL from STOP");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.StopSpeed);
                    driveCommand.set(Speed.LEVEL, Direction.UP);
                    break;
                case 9:
                    Harness.log(name, "Drivespeed adjacent: STOP from LEVEL");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.LevelingSpeed);
                    driveCommand.set(Speed.STOP, Direction.UP);
                    break;
                case 10:
                    Harness.log(name, "Drivespeed adjacent: SLOW from STOP");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.StopSpeed);
                    driveCommand.set(Speed.SLOW, Direction.UP);
                    break;
                case 11:
                    Harness.log(name, "Drivespeed adjacent: SLOW from LEVEL");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.LevelingSpeed);
                    driveCommand.set(Speed.SLOW, Direction.UP);
                    break;
                case 12:
                    Harness.log(name, "Drivespeed adjacent: FAST from SLOW");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    physicalConnection.sendOnce(driveSpeed);
                    //wait one period before setting the command
                    Timer t = new Timer(new TimeSensitive() {

                        public void timerExpired(Object callbackData) {
                            driveCommand.set(Speed.FAST, Direction.UP);
                        }
                    });
                    t.start(period);
                    //driveCommand.set(Speed.FAST, Direction.UP);
                    break;
                case 13:
                    Harness.log(name, "Drivespeed adjacent: STOP from SLOW");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    driveCommand.set(Speed.STOP, Direction.UP);
                    break;
                case 14:
                    Harness.log(name, "Drivespeed adjacent: LEVEL from SLOW");
                    expectException = false;
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    driveCommand.set(Speed.LEVEL, Direction.UP);
                    break;

                case 15:
                    Harness.log(name, "FRONT Reversal");
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(DoorCommand.CLOSE);
                    break;
                case 16:
                    Harness.log(name, "BACK Reversal");
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(DoorCommand.CLOSE);
                    break;
                case 17:
                    Harness.log(name, "FRONT Reversal canceled by open");
                    expectException = false;
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(DoorCommand.CLOSE);
                    timer.start(REVERSAL_CANCEL_PERIOD, new Integer(whichTest));
                    break;
                case 18:
                    Harness.log(name, "BACK Reversal canceled by open");
                    expectException = false;
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(DoorCommand.CLOSE);
                    timer.start(REVERSAL_CANCEL_PERIOD, new Integer(whichTest));
                    break;
                case 19:
                    Harness.log(name, "FRONT Reversal canceled by nudge");
                    expectException = false;
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(DoorCommand.CLOSE);
                    timer.start(REVERSAL_CANCEL_PERIOD, new Integer(whichTest));
                    break;
                case 20:
                    Harness.log(name, "BACK Reversal canceled by nudge");
                    expectException = false;
                    doorReversals[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(true);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(DoorCommand.CLOSE);
                    doorMotors[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(DoorCommand.CLOSE);
                    timer.start(REVERSAL_CANCEL_PERIOD, new Integer(whichTest));
                    break;
                case 21:
                    Harness.log(name, "Door Open with no landing");
                    atFloor[ReplicationComputer.computeReplicationId(2, Hallway.BACK)].set(true);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(false);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(false);
                    break;
                case 22:
                    Harness.log(name, "Door Open while slow");
                    driveSpeed.set(Direction.UP, DriveObject.SlowSpeed);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.LEFT)].set(false);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.FRONT, Side.RIGHT)].set(false);
                    break;
                case 23:
                    Harness.log(name, "Door Open while leveling");
                    expectException = false;
                    atFloor[ReplicationComputer.computeReplicationId(2, Hallway.BACK)].set(true);
                    driveSpeed.set(Direction.UP, DriveObject.LevelingSpeed);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.LEFT)].set(false);
                    doorCloseds[ReplicationComputer.computeReplicationId(Hallway.BACK, Side.RIGHT)].set(false);
                    break;
                default:
                    throw new RuntimeException("Unknown test " + whichTest);
            }

        }

        public int getTestCount() {
            return 25;
        }

        public boolean getExpectException() {
            return expectException;
        }
    }

    /**
     * Main for safety testing.  Note that because of the way Harness is configured,
     * the simulator does not really support multiple test runs in a single execution.
     *
     * You can execute all the tests with the following simple bash script:
    ************************************************
    #!/bin/bash

    for i in `seq 0 23`; do
        java simulator.elevatormodules.Safety $i
    done
    ************************************************
     * If the number of tests changes,the arguments to the seq command should also change.
     *
     * @param args
     */
    public static void main(String[] args) {

        Harness.setRandomSeed(System.currentTimeMillis());
        Harness.setRealtimeRate(100.0);
        Harness.initialize(SimTime.ZERO, false, false, false, false);

        final SimTime SIM_RUN_TIME = new SimTime(20, SimTimeUnit.SECOND);

        int whichTest = Integer.parseInt(args[0]);
        //int whichTest = 23;

        Safety safety = new Safety();
        SafetyTest test = new SafetyTest(whichTest);

        try {
            Harness.runSim(SIM_RUN_TIME);
            if (test.getExpectException()) {
                System.err.println("Test failed.  Expected an exception but none was thrown.");
            } else {
                System.out.println("Test passed.  No exception thrown.");
            }
        } catch (SafetyViolationException ex) {
            if (test.getExpectException()) {
                System.out.println("Test passed.  Exception:" + ex.getMessage());
            } else {
                System.err.println("Test failed.  Unexpected exception: " + ex.getMessage());
            }
        }
        //System.out.println(safety.)

    }
}
