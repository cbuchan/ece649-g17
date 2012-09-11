/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;
import simulator.payloads.CarCallPayload;
import simulator.payloads.CarCallPayload.WriteableCarCallPayload;
import simulator.payloads.CarLightPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;

/**
 * implementation of the physical button and light
 * The main purpose of this class is to provide an interface for the
 * passenger objects.
 *
 * @author Justin Ray
 */
public class CarButtonLight extends Module implements TimeSensitive {

    private final int floor;
    private final Hallway hallway;
    private final WriteableCarCallPayload callButtonState;
    private final ReadableCarLightPayload localLight;
    public final static SimTime BUTTON_PERIOD = new SimTime(20, SimTime.SimTimeUnit.MILLISECOND);
    private final Timer timer = new Timer(this);

    public CarButtonLight(int floor, Hallway hallway, boolean verbose) {
        super(BUTTON_PERIOD, "CarButtonLight" + ReplicationComputer.makeReplicationString(floor, hallway), verbose);
        this.floor = floor;
        this.hallway = hallway;

        localLight = CarLightPayload.getReadablePayload(floor, hallway);
        physicalConnection.registerEventTriggered(localLight);
        callButtonState = CarCallPayload.getWriteablePayload(floor, hallway);
        callButtonState.set(false);
        physicalConnection.sendTimeTriggered(callButtonState, period);
        sendButtonMessage();
    }

    @Override
    public void receive(CarLightPayload.ReadableCarLightPayload msg) {
        
    }

    public boolean isLighted() {
        return localLight.lighted();
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

}
