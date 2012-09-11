package simulator.payloads;

/**
 * Supplies emergency braking in case of safety violation such as hoistway
 * limit over-run or movement with doors open. One per Car. Can be used
 * exactly one time, after which elevator hoistway requires significant repair
 * maintenance. Triggering the EmergencyBrake in simulation means that either
 * a safety-critical sensor/actuator has been broken or your elevator
 * controller has attempted unsafe operation. (If the EmergencyBrake activates
 * during your final project demo due to an attempt of unsafe operation, there
 * will be a scoring penalty.)  Initialized to <code>false</code>.
 *
 * @author jdevale
 */
public class EmergencyBrakePayload extends PhysicalPayload implements InternalPayload {

    private boolean isBraking;

    public static final class ReadableEmergencyBrakePayload extends PhysicalReadablePayload {

        private final EmergencyBrakePayload payload;

        private ReadableEmergencyBrakePayload(EmergencyBrakePayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * 
         * @return current state of the brake - true if braking
         */
        public boolean isBraking() {
            return payload.isBraking;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableEmergencyBrakePayload extends PhysicalWriteablePayload {

        private final EmergencyBrakePayload payload;

        private WriteableEmergencyBrakePayload(EmergencyBrakePayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * 
         * @return current state of the brake - true if braking
         */
        public boolean isBraking() {
            return payload.isBraking;
        }

        /**
         * Set the braking value
         * @param isBraking true if braking
         */
        public void set(boolean isBraking) {
            payload.set(isBraking);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableEmergencyBrakePayload getWriteablePayload() {
        return new WriteableEmergencyBrakePayload(new EmergencyBrakePayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableEmergencyBrakePayload getReadablePayload() {
        return new ReadableEmergencyBrakePayload(new EmergencyBrakePayload());
    }

    EmergencyBrakePayload() {
        super(PhysicalPayload.EmergencyBrakeEvent);
        isBraking = false;
        setName("EmergencyBrakePayload");
    }

    public EmergencyBrakePayload set(boolean value) {
        this.isBraking = value;
        return this;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        isBraking = ((EmergencyBrakePayload) src).isBraking;

    }

    @Override
    public String toString() {
        return super.toString() + "(" + isBraking + ")";
    }

    @Override
    public Payload clone() {
        EmergencyBrakePayload c = new EmergencyBrakePayload();
        c.copyFrom(this);
        return c;
    }
}
