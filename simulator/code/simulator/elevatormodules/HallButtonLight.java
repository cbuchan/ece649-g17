/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.framework.Direction;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;
import simulator.payloads.HallCallPayload;
import simulator.payloads.HallCallPayload.WriteableHallCallPayload;
import simulator.payloads.HallLightPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;

/**
 * implementation of the physical button and light
 * The main purpose of this class is to provide an interface for the
 * passenger objects.
 *
 * @author Justin Ray
 */
public class HallButtonLight extends Module implements TimeSensitive {

    private final int floor;
    private final Hallway hallway;
    private final Direction direction;
    private final WriteableHallCallPayload callButtonState;
    private final ReadableHallLightPayload localLightState;
    public final static SimTime BUTTON_PERIOD = new SimTime(20, SimTime.SimTimeUnit.MILLISECOND);
    private final Timer timer = new Timer(this);

    public HallButtonLight(int floor, Hallway hallway, Direction direction, boolean verbose) {
        super(BUTTON_PERIOD, "HallButtonLight" + ReplicationComputer.makeReplicationString(floor, hallway, direction), verbose);
        this.floor = floor;
        this.hallway = hallway;
        this.direction = direction;

        localLightState = HallLightPayload.getReadablePayload(floor, hallway, direction);
        physicalConnection.registerEventTriggered(localLightState);
        callButtonState = HallCallPayload.getWriteablePayload(floor, hallway, direction);
        callButtonState.set(false);
        physicalConnection.sendTimeTriggered(callButtonState, period);
        sendButtonMessage();
    }

    @Override
    public void receive(HallLightPayload.ReadableHallLightPayload msg) {
    }

    public boolean isLighted() {
        return localLightState.lighted();
    }

    public void press(SimTime pressTime) {
        if (!pressTime.isPositive()) {
            throw new RuntimeException("Press time must be greater than 0, not " + pressTime);
        }
        callButtonState.set(true);
        sendButtonMessage();
        timer.start(pressTime);
    }

    /**
     * Use for manual release
     */
    public void release() {
        callButtonState.set(false);
        sendButtonMessage();
    }

    /**
     * When timer expires, release the button.
     * @param callbackData - not used
     */
    public void timerExpired(Object callbackData) {
        callButtonState.set(false);
        sendButtonMessage();
    }


    public boolean isPressed() {
        return callButtonState.pressed();
    }

    private void sendButtonMessage() {
        physicalConnection.sendOnce(callButtonState);
    }

    /**
     * @return the floor
     */
    public int getFloor() {
        return floor;
    }

    /**
     * @return the hallway
     */
    public Hallway getHallway() {
        return hallway;
    }

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

}
