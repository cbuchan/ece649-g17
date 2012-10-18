/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

/**
 * @author Rajeev Sharma (rdsharma)
 */
public class DesiredDwellCanPayloadTranslator extends simulator.payloads.translators.CanPayloadTranslator {

    final String name;

    /**
     * CAN payload translator for desired dwell network message
     *
     * @param p CAN payload object whose message is interpreted by this translator
     * @param hallway Hallway to this translator is associated with
     */
    public DesiredDwellCanPayloadTranslator(simulator.payloads.CanMailbox.WriteableCanMailbox p,
            simulator.framework.Hallway hallway) {
        super(p, 4, MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(hallway));
        this.name = "DesiredDwell"
                + simulator.framework.ReplicationComputer.makeReplicationString(hallway);
    }

    /**
     * CAN payload translator for car weight network message
     *
     * @param p CAN payload object whose message is interpreted by this translator
     * @param hallway Hallway to this translator is associated with
     */
    public DesiredDwellCanPayloadTranslator(simulator.payloads.CanMailbox.ReadableCanMailbox p,
            simulator.framework.Hallway hallway) {
        super(p, 4, MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(hallway));
        this.name = "DesiredDwell"
                + simulator.framework.ReplicationComputer.makeReplicationString(hallway);
    }

    //required for reflection
    public void set(int value) {
        setValue(value);
    }


    public int getValue() {
        return getIntFromBitset(getMessagePayload(), 0, getByteSize() * 4);
    }

    public void setValue(int position) {
        java.util.BitSet b = getMessagePayload();
        addIntToBitset(b, position, 0, getByteSize() * 4);
        setMessagePayload(b, getByteSize());
    }

    @Override
    public String payloadToString() {
        return name + " = " + getValue();
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
