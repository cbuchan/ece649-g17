package simulator.elevatormodules;

import simulator.elevatorcontrol.MessageDictionary;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.elevatormodules.VarIntegerCanTranslator;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * @author Rajeev Sharma (rdsharma)
 */
public class DesiredDwellCanPayloadTranslator extends VarIntegerCanTranslator {

    /**
     * CAN payload translator for desired dwell network message
     *
     * @param p CAN payload object whose message is interpreted by this translator
     */
    public DesiredDwellCanPayloadTranslator(WriteableCanMailbox p, Hallway hallway) {
        super(p, MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway),
                "DesiredDwell" + ReplicationComputer.makeReplicationString(hallway), 2);
    }

    /**
     * CAN payload translator for car weight network message
     *
     * @param p CAN payload object whose message is interpreted by this translator
     */
    public DesiredDwellCanPayloadTranslator(ReadableCanMailbox p, Hallway hallway) {
        super(p, MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway),
                "DesiredDwell" + ReplicationComputer.makeReplicationString(hallway), 2);
    }

    /**
     * Get the value of Dwell contained in the message.
     *
     * @return Dwell
     */
    public int getDwell() {
        return getValue();
    }

    /**
     * Set the value of Dwell contained in the message.
     *
     * @param dwell Desired dwell
     */
    public void setDwell(int dwell) {
        setValue(dwell);
    }
}
