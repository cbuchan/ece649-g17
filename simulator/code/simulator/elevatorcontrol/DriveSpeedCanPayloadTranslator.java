/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

import java.util.BitSet;
import simulator.framework.Direction;
import simulator.framework.Speed;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

/**
 * Can payload translator for the DriveSpeed command, which includes a speed value and a direction.
 * Identical to the can payload translator for Drive command.
 * 
 * See the documentation for DesiredFloorCanPayloadTranslator for more discussion
 * on CanPayloadTranslators in general.
 * 
 * @author Justin Ray
 */
public class DriveSpeedCanPayloadTranslator extends CanPayloadTranslator {

    public DriveSpeedCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, 8, MessageDictionary.DRIVE_SPEED_CAN_ID);
    }
    
    public DriveSpeedCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, 8, MessageDictionary.DRIVE_SPEED_CAN_ID);
    }

    /**
     * This method is required for setting values by reflection in the
     * MessageInjector.  The order of parameters in .mf files should match the
     * signature of this method.
     * All translators must have a set() method with the signature that contains
     * all the parameter values.
     *
     * @param speed
     * @param dir
     */
    public void set(double speed, Direction dir) {
        setSpeed(speed);
        setDirection(dir);
    }
    
    public void setSpeed(double speed) {
        BitSet b = getMessagePayload();
        addDoubleToBitset(b, speed, 0, 64);
        setMessagePayload(b, getByteSize());
    }

    public double getSpeed() {
        double val = getDoubleFromBitset(getMessagePayload(), 0, 64);
        return val;
    }

    public void setDirection(Direction dir) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, dir.ordinal(), 32, 32);
        setMessagePayload(b, getByteSize());
    }

    public Direction getDirection() {
        int val = getIntFromBitset(getMessagePayload(), 32, 32);
        for (Direction d : Direction.values()) {
            if (d.ordinal() == val) {
                return d;
            }
        }
        throw new RuntimeException("Unrecognized Direction Value " + val);
    }

    @Override
    public String payloadToString() {
        return "DriveSpeed:  speed=" + getSpeed() + " direction=" + getDirection();
    }
}
