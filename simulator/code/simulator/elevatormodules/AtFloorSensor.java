package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.AtFloorPayload.WriteableAtFloorPayload;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;

/**
 * System object that detects when the Car is within a specified distance from a floor landing.
 * This sensor sends AtFloor messages on the framework and the network to
 * indicate whether the Car is within the specified range.
 */

public class AtFloorSensor extends Module
{

    public final static double AT_FLOOR_OFFSET = 0.08;

    private double bottomPosition;
    private double topPosition;

    private WriteableAtFloorPayload localState;
    private WriteableCanMailbox localStateNwk;
    private ReadableCarPositionPayload carPosition;
    private AtFloorCanPayloadTranslator localStateTranslator;
    private ReadableDriveSpeedPayload localSpeed;

    /**
     * Creates a sensor that detects when the car is near the specified floor.
     * When the Car position is between <code>bottom</code> and
     * <code>top</code>, inclusive, the Car is considered to be "at" this
     * floor.
     *
     * @param period
     * time in microseconds between AtFloor messages
     */
    public AtFloorSensor (SimTime period, int floor, Hallway hallway) {
        super(period, "AtFloor["+floor+","+hallway+"]", false);

        bottomPosition = (floor-1)*Elevator.DISTANCE_BETWEEN_FLOORS - AT_FLOOR_OFFSET;
        topPosition = (floor-1)*Elevator.DISTANCE_BETWEEN_FLOORS + AT_FLOOR_OFFSET;;

        localState = AtFloorPayload.getWriteablePayload(floor, hallway);
        localSpeed = DriveSpeedPayload.getReadablePayload();
        carPosition = CarPositionPayload.getReadablePayload();
        localStateNwk = CanMailbox.getWriteableCanMailbox(MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));
        localStateTranslator = new AtFloorCanPayloadTranslator(localStateNwk, floor, hallway);       

        physicalConnection.registerEventTriggered(carPosition);
        physicalConnection.registerEventTriggered(localSpeed);

        canNetworkConnection.sendTimeTriggered(localStateNwk, period);
        physicalConnection.sendTimeTriggered(localState, period);
    }

    @Override
    public void receive(ReadableDriveSpeedPayload p)
    {
      //the drive speed is automatically updated by the 
    }

    @Override
    public void receive(ReadableCarPositionPayload msg)
    {
        boolean previousValue = localState.value();
        //convert mm to meters
        double position = carPosition.position();

        localState.set(bottomPosition <= position && (position <= topPosition) && localSpeed.speed() <= DriveObject.SlowSpeed);
        /*localState.value = (bottomPosition <= carPosition.position)
	&& (carPosition.position <= topPosition);*/

        if(previousValue != localState.value()) {
            // BL 10/22/02 removed this since we now care about bandwidth
            //canNetworkConnection.SendNetworkMessage(localState);
            physicalConnection.sendOnce(localState);
        }
        //copy to network
        localStateTranslator.setValue(localState.value());
    }
}
