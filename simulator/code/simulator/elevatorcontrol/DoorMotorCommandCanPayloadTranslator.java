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

import simulator.framework.DoorCommand;
import simulator.framework.Hallway;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

import java.util.BitSet;

/**
 *
 * @author rajeev
 */
public class DoorMotorCommandCanPayloadTranslator extends CanPayloadTranslator {
    private final Hallway hallway;
    private final Side side;
    
    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorMotorCommandCanPayloadTranslator(WriteableCanMailbox payload, Hallway hallway, Side side) {
        super(payload, 1, MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
    }

    /**
     * CAN payload translator for door reversal network messages
     * @param payload  CAN payload object whose message is interpreted by this translator
     * @param hallway  replication index
     * @param side  replication index
     */
    public DoorMotorCommandCanPayloadTranslator(ReadableCanMailbox payload, Hallway hallway, Side side) {
        super(payload, 1, MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID + ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
    }
    
    /**
     * Get the hallway associated with this DoorMotorCommand message.
     * @return Hallway
     */
    public Hallway getHallway() {
        return hallway;
    }
    
    /**
     * Get the side associated with this DoorMotorCommand message.
     * @return Side
     */
    public Side getSide() {
        return side;
    }
    
    /**
     * Set the command in the CAM payload.
     * @param command DoorCommand to set
     */
    public void set(DoorCommand command) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, command.ordinal(), 0, 8);
        setMessagePayload(b, getByteSize());
    }
    
    /**
     * Get the command contained in the CAM payload.
     * @return DoorCommand currently set
     */
    public DoorCommand getCommand() {
        int val = getIntFromBitset(getMessagePayload(), 0, 8);
        for (DoorCommand command : DoorCommand.values()) {
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
