package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.DoorOpenPayload.WriteableDoorOpenPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;

/**
 * Sends network messages to indicate whether each
 * door is greater than 99% open.  If a door is greater than 99% open, the
 * corresponding message value is <code>true</code>, otherwise the message
 * value is <code>false</code>.  A separate message is sent for each door.
 */

public class DoorOpenedSensor extends Module {

    private final static int OPEN_THRESHOLD = 49;

    
    double doorPosition;
    boolean previousState;

    // Physical door state
    WriteableDoorOpenPayload physicalPayload;
    //network door state
    WriteableCanMailbox networkPayload;
    DoorOpenedCanPayloadTranslator networkTranslator;

    /**
     * @param period
     * time in microseconds between network messages
     */
    public DoorOpenedSensor(SimTime period, Hallway hallway, Side side) {
        super(period, "DoorOpenedSensor(" + hallway + "," + side + ")", false);

        //initialize physical messages
        //appears we don't need physical messages because this module only outputs
        //to the network
        physicalPayload = DoorOpenPayload.getWriteablePayload(hallway, side);

        //initialize Can payloads
        networkPayload = CanMailbox.getWriteableCanMailbox(MessageDictionary.DOOR_OPEN_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side));
        networkTranslator = new DoorOpenedCanPayloadTranslator(networkPayload, hallway, side);
        canNetworkConnection.sendTimeTriggered(networkPayload, period);

        //register to receive door position updates
        physicalConnection.registerEventTriggered(DoorPositionPayload.getReadablePayload(hallway, side));

        // iniitialize doors to all the way Opened 
        doorPosition = 0;
    }

    // event triggered means we always have the most up to date info on the doors' positions 
    @Override
    public void receive(ReadableDoorPositionPayload msg) {
        // track door position to spit out later 
        doorPosition = msg.position();

        //update the network message status
        boolean doorState = doorPosition >= OPEN_THRESHOLD;
        networkTranslator.setValue(doorState);
        physicalPayload.set(doorState);
        
        //Harness.log(name, "doorState = " + doorState);

        if (doorState != previousState) {
            previousState = doorState;
            //send a physical doorOpened message
            physicalConnection.sendOnce(physicalPayload);
        }
    }
} 

