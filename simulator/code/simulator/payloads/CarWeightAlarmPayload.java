package simulator.payloads;

/**
 * Network message indicates whether the alarm inside the car is ringing;
 * framework message is a command for the Car Weight Alarm to turn on or off.
 *
 * @author Charles Shelton
 */
public class CarWeightAlarmPayload extends PhysicalPayload implements InternalPayload {

    private boolean isRinging;

    public static final class ReadableCarWeightAlarmPayload extends PhysicalReadablePayload {

        private final CarWeightAlarmPayload payload;

        private ReadableCarWeightAlarmPayload(CarWeightAlarmPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * 
         * @return the current state of the alarm - true if ringing
         */
        public boolean isRinging() {
            return payload.isRinging;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarWeightAlarmPayload extends PhysicalWriteablePayload {

        private final CarWeightAlarmPayload payload;

        private WriteableCarWeightAlarmPayload(CarWeightAlarmPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return the current state of the alarm - true if ringing
         */
        public boolean isRinging() {
            return payload.isRinging;
        }

        /**
         * Set the current alarm state
         * @param isRinging true if ringing
         */
        public void set(boolean isRinging) {
            payload.set(isRinging);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarWeightAlarmPayload getWriteablePayload() {
        return new WriteableCarWeightAlarmPayload(new CarWeightAlarmPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarWeightAlarmPayload getReadablePayload() {
        return new ReadableCarWeightAlarmPayload(new CarWeightAlarmPayload());
    }

    CarWeightAlarmPayload() {
        super(PhysicalPayload.CarWeightAlarmEvent);
        setName("CarWeightAlarmPayload");
        isRinging = false;
    }

    public CarWeightAlarmPayload set(boolean value) {
        this.isRinging = value;
        return this;
    }

    @Override
    public void copyFrom(Payload src) {
        isRinging = ((CarWeightAlarmPayload) src).isRinging;
        super.copyFrom(src);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + isRinging + ")";
    }

    @Override
    public Payload clone() {
        CarWeightAlarmPayload c = new CarWeightAlarmPayload();
        c.copyFrom(this);
        return c;
    }
}
