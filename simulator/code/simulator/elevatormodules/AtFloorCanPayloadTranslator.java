package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class AtFloorCanPayloadTranslator extends BooleanCanTranslator {

    /**
     * CAN translator for messages from atfloor sensors
     * @param payload CAN payload object whose message is interpreted by this translator
     * @param floor replication index
     * @param hallway replication index
     */
    public AtFloorCanPayloadTranslator(WriteableCanMailbox payload, int floor, Hallway hallway) {
        super(payload, MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway), "AtFloor" + ReplicationComputer.makeReplicationString(floor, hallway));
    }

    /**
     * CAN translator for messages from atfloor sensors
     * @param payload CAN payload object whose message is interpreted by this translator
     * @param floor replication index
     * @param hallway replication index
     */
    public AtFloorCanPayloadTranslator(ReadableCanMailbox payload, int floor, Hallway hallway) {
        super(payload, MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway), "AtFloor" + ReplicationComputer.makeReplicationString(floor, hallway));
    }

}
