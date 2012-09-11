package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Direction;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class LevelingCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN payload translator for leveling network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public LevelingCanPayloadTranslator(WriteableCanMailbox payload, Direction direction) {
        super(payload, MessageDictionary.LEVELING_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction),
                "LevelingSensor" + ReplicationComputer.makeReplicationString(direction));
    }

    /**
     * CAN payload translator for leveling network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public LevelingCanPayloadTranslator(ReadableCanMailbox payload, Direction direction) {
        super(payload, MessageDictionary.LEVELING_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction),
                "LevelingSensor" + ReplicationComputer.makeReplicationString(direction));
    }
}
