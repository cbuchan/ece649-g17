/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal) - Author
 * Rajeev Sharma (rdsharma)
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.framework.Direction;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 *
 * @author jessesal
 */
public class HallCallCanPayloadTranslator extends BooleanCanTranslator{

    /**
     * CAN payload translator for Hall Call network messages
     * @param p  CAN payload object whose message is interpreted by this translator
	 * @param floor replication index
     * @param hallway  replication index
     * @param dir  replication index
     */
    public HallCallCanPayloadTranslator(WriteableCanMailbox p, int floor, Hallway hallway, Direction dir) {
        super(p,
                MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, dir),
                "HallCallSensor" + ReplicationComputer.makeReplicationString(floor, hallway, dir));
                this.setValue(true);
    }

    /**
     * CAN payload translator for door closed network messages
     * @param p  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public HallCallCanPayloadTranslator(ReadableCanMailbox p, int floor, Hallway hallway, Direction dir) {
        super(p,
                MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, dir),
                "HallCallSensor" + ReplicationComputer.makeReplicationString(floor, hallway, dir));
    }
}
