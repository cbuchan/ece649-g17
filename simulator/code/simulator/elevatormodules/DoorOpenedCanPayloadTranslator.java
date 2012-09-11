package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

public class DoorOpenedCanPayloadTranslator extends BooleanCanTranslator {


    /**
     * CAN translator for door open network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorOpenedCanPayloadTranslator(WriteableCanMailbox p, Hallway hallway, Side side) {
        super(p, MessageDictionary.DOOR_OPEN_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorOpenedSensor" + ReplicationComputer.makeReplicationString(hallway, side));
    }

    /**
     * CAN translator for door open network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorOpenedCanPayloadTranslator(ReadableCanMailbox p, Hallway hallway, Side side) {
        super(p, MessageDictionary.DOOR_OPEN_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorOpenedSensor" + ReplicationComputer.makeReplicationString(hallway, side));
    }

}
