/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatormodules.passengers.events.DoorOpeningEvent;
import simulator.framework.DoorCommand;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.DoorClosedPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorPositionPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;

/**
 * provides a passenger interface to the doors
 * @author justinr2
 */
public class Door extends PassengerModule {

    private final Hallway hallway;
    private final ReadableDoorPositionPayload leftPos;
    private final ReadableDoorPositionPayload rightPos;
    private final ReadableDoorMotorPayload leftCommand;
    private final ReadableDoorMotorPayload rightCommand;
    private final ReadableDoorClosedPayload leftDoorClosed;
    private final ReadableDoorClosedPayload rightDoorClosed;
    public final static SimTime DOOR_REVERSAL_PERIOD = new SimTime(10, SimTime.SimTimeUnit.MILLISECOND);
    public final static SimTime REVERSAL_TIME = new SimTime(200, SimTime.SimTimeUnit.MILLISECOND);
    private DoorState doorState = DoorState.CLOSED;

    private final DoorMotor leftDoor;
    private final DoorMotor rightDoor;

    /**
     * @return the hallway
     */
    public Hallway getHallway() {
        return hallway;
    }

    public static enum DoorMotionState {

        STOPPED,
        OPENING,
        CLOSING,
        UNCERTAIN //use this state for when the doors are not doing something consistent
    }

    public static enum DoorState {

        CLOSED,
        NOT_CLOSED
    }

    public Door(Hallway hallway, boolean verbose) {
        super(SimTime.ZERO, "Door" + ReplicationComputer.makeReplicationString(hallway), verbose);

        leftDoor = new DoorMotor(hallway, Side.LEFT, verbose);
        rightDoor = new DoorMotor(hallway, Side.RIGHT, verbose);

        this.hallway = hallway;

        leftPos = DoorPositionPayload.getReadablePayload(hallway, Side.LEFT);
        physicalConnection.registerEventTriggered(leftPos);
        rightPos = DoorPositionPayload.getReadablePayload(hallway, Side.RIGHT);
        physicalConnection.registerEventTriggered(rightPos);
        leftCommand = DoorMotorPayload.getReadablePayload(hallway, Side.LEFT);
        physicalConnection.registerEventTriggered(leftCommand);
        rightCommand = DoorMotorPayload.getReadablePayload(hallway, Side.RIGHT);
        physicalConnection.registerEventTriggered(rightCommand);
        rightDoorClosed = DoorClosedPayload.getReadablePayload(hallway, Side.RIGHT);
        physicalConnection.registerEventTriggered(rightDoorClosed);
        leftDoorClosed = DoorClosedPayload.getReadablePayload(hallway, Side.LEFT);
        physicalConnection.registerEventTriggered(leftDoorClosed);
    }

    public double getWidth() {
        return leftPos.position() + rightPos.position();
    }

    public boolean isNotClosed() {
        return !isClosed();
    }

    public boolean isClosed() {
        return (leftDoorClosed.isClosed() && rightDoorClosed.isClosed());
    }

    private void updateDoorState() {
        DoorState prevState = doorState;
        if (isClosed()) {
            doorState = DoorState.CLOSED;
        } else {
            doorState = DoorState.NOT_CLOSED;
            if (prevState == DoorState.CLOSED) {
                firePassengerEvent(new DoorOpeningEvent(hallway));
            }
        }
    }

    public DoorState getDoorState() {
        return doorState;
    }

    public DoorMotionState getCurrentMotionState() {
        //check consistent conditions first
        if (leftCommand.command() == DoorCommand.STOP && rightCommand.command() == DoorCommand.STOP) {
            //both doors stopped
            return DoorMotionState.STOPPED;
        } else if (leftCommand.command() == DoorCommand.OPEN && rightCommand.command() == DoorCommand.OPEN) {
            //both doors opening
            return DoorMotionState.OPENING;
        } else if (leftCommand.command() == DoorCommand.CLOSE && rightCommand.command() == DoorCommand.CLOSE) {
            //both doors closing
            return DoorMotionState.CLOSING;
        } //check inconsistent conditions
        else if ((leftCommand.command() == DoorCommand.OPEN && rightCommand.command() == DoorCommand.STOP)
                || (leftCommand.command() == DoorCommand.STOP && rightCommand.command() == DoorCommand.OPEN)) {
            //one door opened and the other stopped --> Opening
            return DoorMotionState.OPENING;
        } else if ((leftCommand.command() == DoorCommand.CLOSE && rightCommand.command() == DoorCommand.STOP)
                || (leftCommand.command() == DoorCommand.STOP && rightCommand.command() == DoorCommand.CLOSE)) {
            return DoorMotionState.CLOSING;
        } else {
            //an inconsistent state
            return DoorMotionState.UNCERTAIN;
        }
    }

    
    public boolean block(double width) {
        if (leftDoor.getBlockWidth() > 0 || rightDoor.getBlockWidth() > 0) {
            return false;
        }
        log("Blocking door " + width);
        leftDoor.blockDoor(width/2);
        rightDoor.blockDoor(width/2);
        return true;
    }

    public void unblock() {
        log("Unblocking door");
        leftDoor.unblockDoor();
        rightDoor.unblockDoor();
    }

    public boolean isReversing() {
        //the left and right should be set the same, so just check the left reverse
        return leftDoor.isReversing() && rightDoor.isReversing();
    }

    @Override
    public void receive(ReadableDoorMotorPayload msg) {
        //implement to update
        updateDoorState();
    }

    @Override
    public void receive(ReadableDoorPositionPayload msg) {
        updateDoorState();
    }

    @Override
    public void receive(ReadableDoorClosedPayload msg) {
        updateDoorState();

    }


}
