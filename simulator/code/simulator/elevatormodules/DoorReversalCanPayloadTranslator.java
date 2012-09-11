package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class DoorReversalCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorReversalCanPayloadTranslator(WriteableCanMailbox payload, Hallway hallway, Side side) {
        super(payload, MessageDictionary.DOOR_REVERSAL_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorReversal" + ReplicationComputer.makeReplicationString(hallway, side));
    }

    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorReversalCanPayloadTranslator(ReadableCanMailbox payload, Hallway hallway, Side side) {
        super(payload, MessageDictionary.DOOR_REVERSAL_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorReversal" + ReplicationComputer.makeReplicationString(hallway, side));
    }
}
