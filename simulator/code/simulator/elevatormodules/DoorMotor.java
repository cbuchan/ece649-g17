package simulator.elevatormodules;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.payloads.*;
import simulator.framework.*;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorPositionPayload.WriteableDoorPositionPayload;
import simulator.payloads.DoorReversalPayload.WriteableDoorReversalPayload;

/**
 * Models the doors of the elevator in the simulation.  Each object
 * instatiated models one door.  The Doors open and close via a simple
 * velocity profile (they cannot instaneously change direction) and update
 * their position to the outside world by sending DoorPosition messages.
 **/
public class DoorMotor extends Module implements TimeSensitive {

    public static final SimTime MESSAGE_PERIOD = new SimTime(40, SimTimeUnit.MILLISECOND);
    public static final SimTime CONTROL_PERIOD = new SimTime(40, SimTimeUnit.MILLISECOND);
    public static final int FULLY_CLOSED_WIDTH = 0;
    public static final int FULLY_OPEN_WIDTH = 50;
    public static final int CLOSED_WIDTH_THRESHOLD = 2;
    public static final int OPEN_WIDTH_THRESHOLD = 48;
    public static final double MOVE_INCREMENT = 1.0;
    public static final double NUDGE_INCREMENT = 0.1;


    private double doorPosition = 0;
    private double blockWidth = -1;
    private DoorCommand previousCommand = DoorCommand.STOP;


    Side door;
    Hallway hallway;
    private final ReadableDoorMotorPayload localCommand;
    private final WriteableDoorPositionPayload localDoorPosition;
    private final WriteableDoorReversalPayload reversal;
    private final Timer timer;

    public DoorMotor(Hallway hallway, Side side, boolean verbose) {
        //don't use the period variable here
        super(SimTime.ZERO, "DoorMotor[" + hallway + "," + side + "]", verbose);
        this.hallway = hallway;
        this.door = side;

        localCommand = DoorMotorPayload.getReadablePayload(hallway, side);
        localDoorPosition = DoorPositionPayload.getWriteablePayload(hallway, side);
        reversal = DoorReversalPayload.getWriteablePayload(hallway, side);

        physicalConnection.registerEventTriggered(localCommand);
        physicalConnection.registerTimeTriggered(localCommand);

        physicalConnection.sendTimeTriggered(localDoorPosition, MESSAGE_PERIOD);
        physicalConnection.sendTimeTriggered(reversal, MESSAGE_PERIOD);

        // door is closed and stopped at initialization
        doorPosition = 0;

        timer = new Timer(this);
        timer.start(CONTROL_PERIOD);
    }

    public void timerExpired(Object callBackData) {
        if (previousCommand != localCommand.command()) {
            log("New command = " + localCommand.command());
        }
        switch(localCommand.command()) {
            case NUDGE:
                //only close if the door is not blocked, not closed
                //require two consecutive commands, which introduces one cycle delay
                //that models non-instantaneous direction change.
                if (!(doorPosition <= blockWidth) &&
                        doorPosition > FULLY_CLOSED_WIDTH &&
                        (previousCommand == DoorCommand.CLOSE || previousCommand == DoorCommand.NUDGE)) {
                    doorPosition -= NUDGE_INCREMENT;
                }
                break;
            case CLOSE:
                //only close if the door is not blocked, not closed
                //require two consecutive commands, which introduces one cycle delay
                //that models non-instantaneous direction change.
                if (!(doorPosition <= blockWidth) &&
                        doorPosition > FULLY_CLOSED_WIDTH &&
                        (previousCommand == DoorCommand.CLOSE || previousCommand == DoorCommand.NUDGE)) {
                    doorPosition -= MOVE_INCREMENT;
                }
                break;
            case OPEN:
                //open if not fully open
                //require two consecutive command to model non-instantaneous direction change
                if (doorPosition < FULLY_OPEN_WIDTH && previousCommand == DoorCommand.OPEN) {
                    doorPosition += MOVE_INCREMENT;
                }
                break;
            case STOP:
                //do nothing
                break;
        }

        //check for door reversal
        boolean prevReverse = reversal.isReversing();
        if (doorPosition <= blockWidth &&
                (localCommand.command() != DoorCommand.STOP || localCommand.command() != DoorCommand.NUDGE)) {
            reversal.set(true);
        } else {
            reversal.set(false);
        }
        if (prevReverse != reversal.isReversing()){
            physicalConnection.sendOnce(reversal);
        }

        //output possition
        localDoorPosition.set(doorPosition);

        //prepare for next loop
        previousCommand = localCommand.command();
        timer.start(CONTROL_PERIOD);
    }

    void blockDoor(double width) {
        blockWidth = width;
    }

    void unblockDoor() {
        blockWidth = -1;
    }

    double getBlockWidth() {
        return blockWidth;
    }

    boolean isReversing() {
        return reversal.isReversing();
    }

    @Override
    public void receive(ReadableDoorMotorPayload msg) {
        //do nothing
    }


}
