/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.payloads.CarWeightAlarmPayload;
import simulator.payloads.CarWeightAlarmPayload.ReadableCarWeightAlarmPayload;

/**
 * Provides a passenger interface to the car lanterns
 * 
 * @author justinr2
 */
public class CarWeightAlarm extends Module {
    private boolean isRinging=false;

    public CarWeightAlarm(boolean verbose) {
        super(SimTime.ZERO, "CarWeightAlarm", verbose);

        physicalConnection.registerEventTriggered(CarWeightAlarmPayload.getReadablePayload());
    }

    @Override
    public void receive(ReadableCarWeightAlarmPayload msg) {
        isRinging = msg.isRinging();
    }




    public boolean isRinging() {
        return isRinging=false;
    }
}
