/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatormodules.passengers.events.PositionIndicatorChangedEvent;
import simulator.framework.Elevator;
import simulator.payloads.CarPositionIndicatorPayload;
import simulator.payloads.CarPositionIndicatorPayload.ReadableCarPositionIndicatorPayload;

/**
 * Provides a passenger interface to the car lanterns
 * 
 * @author justinr2
 */
public class CarPositionIndicator extends PassengerModule {
    private int currentFloor;
    private int previousFloor;

    public CarPositionIndicator(boolean verbose) {
        super(SimTime.ZERO, "CarPositionIndicator", verbose);

        physicalConnection.registerEventTriggered(CarPositionIndicatorPayload.getReadablePayload());
        currentFloor = previousFloor = 1;
    }

    @Override
    public void receive(ReadableCarPositionIndicatorPayload msg) {
        previousFloor = currentFloor;
        currentFloor = msg.floor();
        if (previousFloor != currentFloor) {
            firePassengerEvent(new PositionIndicatorChangedEvent(currentFloor, previousFloor));
        }
    }

    public int getIndicatedFloor() {
        return currentFloor;
    }

    public boolean isValid() {
        return (currentFloor >= 1 && currentFloor <= Elevator.numFloors);
    }
}
