/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 *
 * @author justinr2
 */
public class DoorClosedCanPayloadTranslator extends BooleanCanTranslator{

    /**
     * CAN payload translator for door closed network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorClosedCanPayloadTranslator(WriteableCanMailbox p, Hallway hallway, Side side) {
        super(p,
                MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorClosedSensor" + ReplicationComputer.makeReplicationString(hallway, side));
                this.setValue(true);
    }

    /**
     * CAN payload translator for door closed network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorClosedCanPayloadTranslator(ReadableCanMailbox p, Hallway hallway, Side side) {
        super(p,
                MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side),
                "DoorClosedSensor" + ReplicationComputer.makeReplicationString(hallway, side));
    }
}
