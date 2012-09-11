package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.*;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.HoistwayLimitPayload.WriteableHoistwayLimitPayload;

/**
 * Listens for <code>CarPositionPayload</code> framework messages and sends
 * network and framework <code>HoistwayLimitPayload</code> messages when the
 * Car exceeds a specified position.
 */
public class HoistwayLimitSensor extends Module {

    protected Direction direction;
    protected double threshold;
    protected WriteableHoistwayLimitPayload localState;
    protected ReadableCarPositionPayload carPosition;
    protected boolean previousValue;
    protected WriteableCanMailbox networkState;
    protected HoistwayLimitSensorCanPayloadTranslator networkStateTranslator;

    /**
     * @param direction
     * DOWN or UP
     *
     * @param threshold
     * when the Car goes beyond this position value, this hoistway limit
     * switch is triggered
     */
    public HoistwayLimitSensor(SimTime period, Direction direction, double threshold) {
        super(period, "HoistwayLimitSensor(" + direction + ")", false);
        this.direction = direction;
        this.threshold = threshold;

        //set up payloadsstate
        localState = HoistwayLimitPayload.getWriteablePayload(direction);
        carPosition = CarPositionPayload.getReadablePayload();
        networkState = CanMailbox.getWriteableCanMailbox(MessageDictionary.HOISTWAY_LIMIT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction));
        networkStateTranslator = new HoistwayLimitSensorCanPayloadTranslator(networkState, direction);
        networkStateTranslator.setValue(localState.exceeded());

        //register for car position updates
        physicalConnection.registerEventTriggered(carPosition);
        //send physical and can state
        canNetworkConnection.sendTimeTriggered(networkState, period);
        physicalConnection.sendTimeTriggered(localState, period);

        previousValue = false;
    }

    // event triggered so we always have the latest CarPosition information
    @Override
    public void receive(ReadableCarPositionPayload carPosition) {
        if ((direction == Direction.UP) && (carPosition.position() >= threshold)) {
            localState.set(true);
        } else if ((direction == Direction.DOWN) && (carPosition.position() <= threshold)) {
            localState.set(true);
        } else {
            localState.set(false);
        }
        //update network state
        networkStateTranslator.setValue(localState.exceeded());

        if (previousValue != localState.exceeded()) {
            if (localState.exceeded()) {
                log("WARNING! Hoistway limit has been reached!");
            }
            previousValue = localState.exceeded();
            //BL 10/22/02 removed since we now care about bandwidth
            // canNetworkConnection.SendNetworkMessage(localState);
            physicalConnection.sendOnce(localState);
        }
    }
}
