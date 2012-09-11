package simulator.payloads;

import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CarCallPayload.ReadableCarCallPayload;
import simulator.payloads.CarLanternPayload.ReadableCarLanternPayload;
import simulator.payloads.CarLevelPositionPayload.ReadableCarLevelPositionPayload;
import simulator.payloads.CarLightPayload.ReadableCarLightPayload;
import simulator.payloads.CarPositionIndicatorPayload.ReadableCarPositionIndicatorPayload;
import simulator.payloads.CarPositionPayload.ReadableCarPositionPayload;
import simulator.payloads.CarWeightAlarmPayload.ReadableCarWeightAlarmPayload;
import simulator.payloads.CarWeightPayload.ReadableCarWeightPayload;
import simulator.payloads.DoorClosedPayload.ReadableDoorClosedPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;
import simulator.payloads.DoorOpenPayload.ReadableDoorOpenPayload;
import simulator.payloads.DoorPositionPayload.ReadableDoorPositionPayload;
import simulator.payloads.DoorReversalPayload.ReadableDoorReversalPayload;
import simulator.payloads.DrivePayload.ReadableDrivePayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;
import simulator.payloads.EmergencyBrakePayload.ReadableEmergencyBrakePayload;
import simulator.payloads.HallCallPayload.ReadableHallCallPayload;
import simulator.payloads.HallLightPayload.ReadableHallLightPayload;
import simulator.payloads.HoistwayLimitPayload.ReadableHoistwayLimitPayload;
import simulator.payloads.LevelingPayload.ReadableLevelingPayload;

/**
 * An adaptor for receiving event triggered messages.  An object that wishes
 * to receive time-triggered messages still needs to extend this class, but it
 * does not need to override any of these methods.  An object that wishes to
 * receive event-triggered messages, however, must extend this class and
 * override the appropriate <code>receive</code> method for each type of
 * message it wishes to receive.  There is no contract for the methods of this
 * class, so subclasses can take any implementation-specific actions.  The
 * implementations in this class throw an
 * <code>UnsupportedOperationException</code>, so if a subclass registers for
 * messages of a certain type but does not override the corresponding
 * <code>receive</code> method, a runtime exception will occur.
 *
 * @author Christopher Martin
 * @author Kenny Stauffer
 */
public abstract class Networkable {

    /**
     * A tag method to indicate that subclasses should override only specialized
     * methods.  Subclasses are supposed to provide methods tailored to the
     * types of messages they wish to receive.  Making this method
     * <tt>final</tt> enforces that contract.
     */
    protected final void receive(ReadablePayload msg) {
        throw new UnsupportedOperationException(
            "this method should never be called.  The method receive(" +
            msg.getClass() + ")" + " should be defined in the Networkable base class.");
    }

    public void receive(ReadableCanMailbox msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableAtFloorPayload msg) {
        throw new UnsupportedOperationException(this + " received " + msg.toString());
    }

    public void receive(ReadableCarCallPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarLanternPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarLevelPositionPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarLightPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarPositionIndicatorPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarPositionPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarWeightAlarmPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableCarWeightPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDoorClosedPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDoorMotorPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDoorOpenPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDoorPositionPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDoorReversalPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDrivePayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableDriveSpeedPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableEmergencyBrakePayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableHallCallPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableHallLightPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableHoistwayLimitPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

    public void receive(ReadableLevelingPayload msg) {
        throw new UnsupportedOperationException(msg.toString());
    }

}
