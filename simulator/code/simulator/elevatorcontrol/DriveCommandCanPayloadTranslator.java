/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

import simulator.framework.Direction;
import simulator.framework.Speed;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

import java.util.BitSet;

/**
 * Can payload translator for the drive command, which includes a speed value and a direction.
 * <p/>
 * See the documentation for DesiredFloorCanPayloadTranslator for more discussion
 * on CanPayloadTranslators in general.
 *
 * @author Justin Ray
 */
public class DriveCommandCanPayloadTranslator extends CanPayloadTranslator {

    public DriveCommandCanPayloadTranslator(WriteableCanMailbox p) {
        super(p, 1, MessageDictionary.DRIVE_COMMAND_CAN_ID);
    }

    public DriveCommandCanPayloadTranslator(ReadableCanMailbox p) {
        super(p, 1, MessageDictionary.DRIVE_COMMAND_CAN_ID);
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
    public void set(Speed speed, Direction dir) {
        setSpeed(speed);
        setDirection(dir);
    }

    public void setSpeed(Speed speed) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, speed.ordinal() - 2, 0, 2);
        setMessagePayload(b, getByteSize());
    }

    public Speed getSpeed() {
        int val = getIntFromBitset(getMessagePayload(), 0, 2) + 2;
        for (Speed s : Speed.values()) {
            if (s.ordinal() == val) {
                return s;
            }
        }
        throw new RuntimeException("Unrecognized Speed Value " + val);
    }

    public void setDirection(Direction dir) {
        BitSet b = getMessagePayload();
        addIntToBitset(b, dir.ordinal() - 2, 2, 2);
        setMessagePayload(b, getByteSize());
    }

    public Direction getDirection() {
        int val = getIntFromBitset(getMessagePayload(), 2, 2) + 2;
        for (Direction d : Direction.values()) {
            if (d.ordinal() == val) {
                return d;
            }
        }
        throw new RuntimeException("Unrecognized Direction Value " + val);
    }

    @Override
    public String payloadToString() {
        return "DriveCommand:  speed=" + getSpeed() + " direction=" + getDirection();
    }
}
