/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatorcontrol;

/**
 *
 * @author rajeev
 */
public class DoorMotorCommandCanPayloadTranslator extends simulator.payloads.translators.CanPayloadTranslator {
    private final simulator.framework.Hallway hallway;
    private final simulator.framework.Side side;

    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorMotorCommandCanPayloadTranslator(simulator.payloads.CanMailbox.WriteableCanMailbox payload, simulator.framework.Hallway hallway, simulator.framework.Side side) {
        super(payload, 2, MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
    }

    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorMotorCommandCanPayloadTranslator(simulator.payloads.CanMailbox.ReadableCanMailbox payload, simulator.framework.Hallway hallway, simulator.framework.Side side) {
        super(payload, 2, MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID + simulator.framework.ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
    }

    /**
     * Get the hallway associated with this DoorMotorCommand message.
     * @return Hallway
     */
    public simulator.framework.Hallway getHallway() {
        return hallway;
    }

    /**
     * Get the side associated with this DoorMotorCommand message.
     * @return Side
     */
    public simulator.framework.Side getSide() {
        return side;
    }

    /**
     * Set the command in the CAM payload.
     * @param command DoorCommand to set
     */
    public void set(simulator.framework.DoorCommand command) {
        java.util.BitSet b = getMessagePayload();
        addIntToBitset(b, command.ordinal(), 0, 16);
        setMessagePayload(b, getByteSize());
    }

    /**
     * Get the command contained in the CAM payload.
     * @return DoorCommand currently set
     */
    public simulator.framework.DoorCommand getCommand() {
        int val = getIntFromBitset(getMessagePayload(), 0, 16);
        for (simulator.framework.DoorCommand command : simulator.framework.DoorCommand.values()) {
            if (val == command.ordinal()) {
                return command;
            }
        }
        throw new RuntimeException("Unrecognized DoorMotorCommand value " + val);
    }
    
     /**
     * Implement a printing method for the translator.
     * @return
     */
    @Override
    public String payloadToString() {
        return "DoorMotorCommand[" + hallway + "][" + side + "] = " + getCommand(); 
    }
}
