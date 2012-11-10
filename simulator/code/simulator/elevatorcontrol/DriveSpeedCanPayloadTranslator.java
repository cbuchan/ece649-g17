/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

import simulator.framework.Direction;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

import java.util.BitSet;

/**
 * Can payload translator for the DriveSpeed command, which includes a speed value and a direction.
 * Identical to the can payload translator for Drive command.
 * <p/>
 * See the documentation for DesiredFloorCanPayloadTranslator for more discussion
 * on CanPayloadTranslators in general.
 *
 * @author Justin Ray
 */
public class DriveSpeedCanPayloadTranslator extends CanPayloadTranslator {

    public DriveSpeedCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, 2, MessageDictionary.DRIVE_SPEED_CAN_ID);
    }

    public DriveSpeedCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, 2, MessageDictionary.DRIVE_SPEED_CAN_ID);
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
        int numBits = 14;
        int offset = (int) Math.pow(2.0, numBits - 1);

        BitSet b = getMessagePayload();
        //addIntToBitset(b, Float.floatToRawIntBits((float) ((speed * 1000))) - offset, 0, 14);
        addIntToBitset(b, (int) (speed * 1000 - offset), 0, 14);
        setMessagePayload(b, getByteSize());
    }

    public double getSpeed() {

        int numBits = 14;
        int offset = (int) Math.pow(2.0, numBits - 1);

        //return (double) Float.intBitsToFloat(getIntFromBitset(getMessagePayload(), 0, 14) + offset) / 1000;
        return (double) (getIntFromBitset(getMessagePayload(), 0, 14) + offset) / 1000;
    }

    public void setDirection(Direction dir) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, dir.ordinal() - 2, 14, 2);
        setMessagePayload(b, getByteSize());
    }

    public Direction getDirection() {
        int val = getIntFromBitset(getMessagePayload(), 14, 2) + 2;
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
