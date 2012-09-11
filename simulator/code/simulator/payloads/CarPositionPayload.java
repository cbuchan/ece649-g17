package simulator.payloads;

/**
 * Internal framework message that indicates the exact position of the Car in
 * the hoistway.
 *
 * @author jdevale
 */
public class CarPositionPayload extends PhysicalPayload implements InternalPayload {

    /**
     * The distance in meters between the floor of the car and level
     * position of the first floor. */
    private double position;

    public static final class ReadableCarPositionPayload extends PhysicalReadablePayload {

        private final CarPositionPayload payload;

        private ReadableCarPositionPayload(CarPositionPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return Position of the car in meteres
         */
        public double position() {
            return payload.position;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarPositionPayload extends PhysicalWriteablePayload {

        private final CarPositionPayload payload;

        private WriteableCarPositionPayload(CarPositionPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return Position of the car in meteres
         */
        public double position() {
            return payload.position;
        }

        /**
         * Set the car position
         * @param position value in meters
         */
        public void set(double position) {
            payload.set(position);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarPositionPayload getWriteablePayload() {
        return new WriteableCarPositionPayload(new CarPositionPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarPositionPayload getReadablePayload() {
        return new ReadableCarPositionPayload(new CarPositionPayload());
    }

    CarPositionPayload() {
        super(PhysicalPayload.CarPositionEvent);
        position = 0.0;
        setName("CarPositionPayload");
    }

    public void set(double value) {
        this.position = value;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        position = ((CarPositionPayload) src).position;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + position + ")";
    }

    @Override
    public Payload clone() {
        CarPositionPayload c = new CarPositionPayload();
        c.copyFrom(this);
        return c;
    }
}
