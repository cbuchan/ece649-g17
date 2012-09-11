package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;

/**
 * This module receives the framework reversals from the passengers and sends them
 * on the network.
 * @author Justin Ray
 */
public class DoorReversalSensor extends Module implements TimeSensitive {

    WriteableCanMailbox networkState;
    DoorReversalCanPayloadTranslator networkStateTranslator;
    ReadableDoorReversalPayload localDoorReversal;
    private boolean isReversing = false;
    Timer reversalStretchTimer = new Timer(this);
    SimTime minimumReversalTime = new SimTime(200, SimTime.SimTimeUnit.MILLISECOND);

    public DoorReversalSensor(SimTime period, Hallway hallway, Side side) {
        super(period, "DoorReversal(" + hallway + "," + side + ")", false);

        networkState = CanMailbox.getWriteableCanMailbox(MessageDictionary.DOOR_REVERSAL_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side));
        networkStateTranslator = new DoorReversalCanPayloadTranslator(networkState, hallway, side);
        networkStateTranslator.setValue(false);
        canNetworkConnection.sendTimeTriggered(networkState, period);


        localDoorReversal = DoorReversalPayload.getReadablePayload(hallway, side);
        physicalConnection.registerEventTriggered(localDoorReversal);
    }

    @Override
    public void receive(ReadableDoorReversalPayload msg) {
        //System.out.println("@" + Harness.getTime() + " DoorReversalSensor: " + msg);
        if (reversalStretchTimer.isRunning()) {
            //reversal shall be sent while the timer is running
            isReversing = true;
        } else {
            if (localDoorReversal.isReversing()) {
                //start the timer to pulse stretch the reversal output
                isReversing = true;
                reversalStretchTimer.start(minimumReversalTime);
            } else {
                //if no timer and the reversal has stopped, then cancel
                isReversing = false;
            }
        }

        networkStateTranslator.setValue(isReversing);
    }

    public void timerExpired(Object callbackData) {
        //when called, the minimum time for the reversal has occured

        //set the state to match the reversal
        //it is okay for us to send it longer
        //than the minimum, just not shorter
        isReversing = localDoorReversal.isReversing();
        
        networkStateTranslator.setValue(isReversing);
    }
}
