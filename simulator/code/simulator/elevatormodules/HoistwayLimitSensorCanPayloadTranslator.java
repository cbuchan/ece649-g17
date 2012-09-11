package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Direction;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class HoistwayLimitSensorCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN payload translator for hoistway limit network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param direction  replication index
     */
    public HoistwayLimitSensorCanPayloadTranslator(WriteableCanMailbox payload, Direction direction) {
        super(payload, MessageDictionary.HOISTWAY_LIMIT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction),
                "HoistwayLimitSensor" + ReplicationComputer.makeReplicationString(direction));
    }

    /**
     * CAN payload translator for hoistway limit network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param direction  replication index
     */
    public HoistwayLimitSensorCanPayloadTranslator(ReadableCanMailbox payload, Direction direction) {
        super(payload, MessageDictionary.HOISTWAY_LIMIT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(direction),
                "HoistwayLimitSensor" + ReplicationComputer.makeReplicationString(direction));
    }
}
