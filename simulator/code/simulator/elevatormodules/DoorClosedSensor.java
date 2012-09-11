package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.DoorClosedPayload.WriteableDoorClosedPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;

/**
 * Sends network messages to indicate whether each
 * door is less than 1% open.  If a door is less than 1% open, the
 * corresponding message value is <code>true</code>, otherwise the message
 * value is <code>false</code>.  A separate message is sent for each door.
 */
public class DoorClosedSensor extends Module {

    double doorPosition;
    boolean previousState;

    // Physical door state
    WriteableDoorClosedPayload physicalPayload;
    //network door state
    WriteableCanMailbox networkPayload;
    DoorClosedCanPayloadTranslator networkTranslator;

    /**
     * @param period
     * time in microseconds between network messages
     */
    public DoorClosedSensor(SimTime period, Hallway hallway, Side side) {
        super(period, "DoorClosedSensor(" + hallway + "," + side + ")", false);

        //initialize physical messages
        //appears we don't need physical messages because this module only outputs
        //to the network
        physicalPayload = DoorClosedPayload.getWriteablePayload(hallway, side);

        //initialize Can payloads
        networkPayload = CanMailbox.getWriteableCanMailbox(MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side));
        networkTranslator = new DoorClosedCanPayloadTranslator(networkPayload, hallway, side);
        networkTranslator.setValue(true);  //initialize to closed
        canNetworkConnection.sendTimeTriggered(networkPayload, period);

        //register to receive door position updates
        physicalConnection.registerEventTriggered(DoorPositionPayload.getReadablePayload(hallway, side));

        // iniitialize doors to all the way CLOSED 
        doorPosition = 0;
    }

    // event triggered means we always have the most up to date info on the doors' positions 
    @Override
    public void receive(ReadableDoorPositionPayload msg) {
        // track door position to spit out later 
        doorPosition = msg.position();

        //update the network message status
        boolean doorState = doorPosition <= 1;
        networkTranslator.setValue(doorState);
        physicalPayload.set(doorState);
        
        if (doorState != previousState) {
            //System.out.println("**********************New Door State: " + name + " = " + doorState);
            //System.out.println("position=" + doorPosition);
            previousState = doorState;
            //send a physical doorclosed message
            physicalConnection.sendOnce(physicalPayload);
        }
    }
} 
