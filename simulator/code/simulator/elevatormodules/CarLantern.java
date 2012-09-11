/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.framework.Direction;
import simulator.payloads.CarLanternPayload;
import simulator.payloads.CarLanternPayload.ReadableCarLanternPayload;

/**
 * Provides a passenger interface to the car lanterns
 * 
 * @author justinr2
 */
public class CarLantern extends Module {

    private Direction currentDirection;
    private final ReadableCarLanternPayload upLantern;
    private final ReadableCarLanternPayload downLantern;

    public CarLantern(boolean verbose) {
        super(SimTime.ZERO, "Car Lanterns", verbose);

        upLantern = CarLanternPayload.getReadablePayload(Direction.UP);
        physicalConnection.registerEventTriggered(upLantern);
        downLantern = CarLanternPayload.getReadablePayload(Direction.DOWN);
        physicalConnection.registerEventTriggered(downLantern);
    }

    @Override
    public void receive(ReadableCarLanternPayload msg) {
        if (upLantern.lighted()) {
            currentDirection = Direction.UP;
        } else if (downLantern.lighted()) {
            currentDirection = Direction.DOWN;
        } else {
            currentDirection = Direction.STOP;
        }
    }

    public Direction getLanternDirection() {
        return currentDirection;
    }
}
