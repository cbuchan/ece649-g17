/* 18649 Fall 2012
 * (Group  17)
 * Jesse Salazar (jessesal)
 * Rajeev Sharma (rdsharma) 
 * Collin Buchan (cbuchan)
 * Jessica Tiu   (jtiu)
 */

package simulator.elevatorcontrol;

/**
 * Can payload translator for the DriveSpeed command, which includes a speed value and a direction.
 * Identical to the can payload translator for Drive command.
 * 
 * See the documentation for DesiredFloorCanPayloadTranslator for more discussion
 * on CanPayloadTranslators in general.
 * 
 * @author Justin Ray
 */
public class DriveSpeedCanPayloadTranslator extends simulator.payloads.translators.CanPayloadTranslator {

    public DriveSpeedCanPayloadTranslator(simulator.payloads.CanMailbox.WriteableCanMailbox p) {
        super(p, 8, MessageDictionary.DRIVE_SPEED_CAN_ID);
    }

    public DriveSpeedCanPayloadTranslator(simulator.payloads.CanMailbox.ReadableCanMailbox p) {
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
    public void set(simulator.framework.Speed speed, simulator.framework.Direction dir) {
        setSpeed(speed);
        setDirection(dir);
    }

    public void setSpeed(simulator.framework.Speed speed) {
        java.util.BitSet b = getMessagePayload();
        addIntToBitset(b, speed.ordinal(), 0, 32);
        setMessagePayload(b, getByteSize());
    }

    public simulator.framework.Speed getSpeed() {
        int val = getIntFromBitset(getMessagePayload(), 0, 32);
        for (simulator.framework.Speed s : simulator.framework.Speed.values()) {
            if (s.ordinal() == val) {
                return s;
            }
        }
        throw new RuntimeException("Unrecognized Speed Value " + val);
    }

    public void setDirection(simulator.framework.Direction dir) {
        java.util.BitSet b = getMessagePayload();
        addIntToBitset(b, dir.ordinal(), 32, 32);
        setMessagePayload(b, getByteSize());
    }

    public simulator.framework.Direction getDirection() {
        int val = getIntFromBitset(getMessagePayload(), 32, 32);
        for (simulator.framework.Direction d : simulator.framework.Direction.values()) {
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
