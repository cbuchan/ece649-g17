/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarPositionPayload;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.LevelingPayload;
import simulator.payloads.LevelingPayload.WriteableLevelingPayload;

/**
 * Implement a physical model of the leveling vane.  There is one object for each
 * direction, UP and DOWN.
 *
 * The leveling sensors are sensors which align with a fixed protrusion near each
 * floor.  If the sensors are correctly aligned with the vane (one above, one below),
 * then the car is precisely level.
 *
 * 
 *   Sensor-->  O       \
 *           -----|     |--SENSOR_OFFSET
 *                |     |
 *       leveling |     /
 *         vane   |  \
 *                |  |-- VANE_HALF_WIDTH
 *           -----|  /
 *   Sensor-->  O 
 *
 * VANE is exacly centered on the floor position.
 *
 * Current constant values are chosen so that one sensor will be blocked by the
 * leveling vane when the atFloor message becomes true.
 *
 * @author justinr2
 */
public class LevelingSensor extends Module {
    public final static double VANE_HALF_WIDTH = 0.04;
    public final static double SENSOR_OFFSET = 0.045;
    public final static double MAX_LEVEL_ERROR = SENSOR_OFFSET - VANE_HALF_WIDTH;

    private final Direction direction;
    private final double sensorOffset; //the sensor offset for this sensor, based on the direction
    private final WriteableLevelingPayload localLeveling;
    private final ReadableCarPositionPayload carPosition;
    private double lastPosition = -1000;  //bogus value, so the first position update always computes values based on current position.
    private final LevelingCanPayloadTranslator mLeveling;

    public LevelingSensor(Direction direction, boolean verbose) {
        super(SimTime.ZERO, "LevelingSensor" + ReplicationComputer.makeReplicationString(direction), verbose);
        this.direction = direction;

        if (direction == Direction.UP) {
            sensorOffset = SENSOR_OFFSET;
        } else if (direction == Direction.DOWN) {
            sensorOffset = -1*SENSOR_OFFSET;
        } else {
            throw new IllegalArgumentException("Direction must be UP or DOWN, not " + direction);
        }

        localLeveling = LevelingPayload.getWriteablePayload(direction);
        localLeveling.set(true); //initialize to true because we assume we start level.
        physicalConnection.sendTimeTriggered(localLeveling, Modules.DRIVE_PERIOD);

        carPosition = CarPositionPayload.getReadablePayload();
        physicalConnection.registerTimeTriggered(carPosition);
        physicalConnection.registerEventTriggered(carPosition);

        WriteableCanMailbox wcm = CanMailbox.getWriteableCanMailbox(MessageDictionary.LEVELING_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction));
        mLeveling = new LevelingCanPayloadTranslator(wcm, direction);
        canNetworkConnection.sendTimeTriggered(wcm, Modules.LEVEL_SENSOR_PERIOD);
    }

    @Override
    public void receive(ReadableCarPositionPayload msg) {

        
        double position = carPosition.position();

        //no need to recompute unless the position has changed
        if (position == lastPosition) return;

        //save the new position
        lastPosition = position;

        /*
         * For each floor, check to see if the sensor is within one VANE_HALF_WIDTH of the
         * floor position.  If so, then the vane is blocking the sensor and the sensor should
         * return false.
         */
        //default is true
        boolean previousLeveling = localLeveling.getValue();
        localLeveling.set(true);
        for (int i=0; i < Elevator.numFloors; i++) {
            double floorPos = i*Elevator.DISTANCE_BETWEEN_FLOORS;
                if (Math.abs((position + sensorOffset) - floorPos) <= VANE_HALF_WIDTH) {
                    //if we find that we are next to a vane, we set to false
                    localLeveling.set(false);
                    break;
                }
        }
        if (localLeveling.getValue() != previousLeveling) {
            physicalConnection.sendOnce(localLeveling);
        }

        //copy to network
        mLeveling.set(localLeveling.value());
    }
}
