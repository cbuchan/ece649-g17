package simulator.payloads;

/**
 * Framework message is a command to the Car Position Indicator to display the
 * specified floor number to the Passengers in the Car.
 *  
 * @author jdevale
 */
public class CarPositionIndicatorPayload extends PhysicalPayload {

    private int floor;

    public static final class ReadableCarPositionIndicatorPayload extends PhysicalReadablePayload {

        private final CarPositionIndicatorPayload payload;

        private ReadableCarPositionIndicatorPayload(CarPositionIndicatorPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return the current floor (from 1 to max number for floors) indicated
         * by the car position indicator
         */
        public int floor() {
            return payload.floor;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarPositionIndicatorPayload extends PhysicalWriteablePayload {

        private final CarPositionIndicatorPayload payload;

        private WriteableCarPositionIndicatorPayload(CarPositionIndicatorPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return the current floor (from 1 to max number for floors) indicated
         * by the car position indicator
         */
        public int floor() {
            return payload.floor;
        }

        /**
         * Set the current floor value
         * @param floor value from 1 to max number of floors
         */
        public void set(int floor) {
            payload.set(floor);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarPositionIndicatorPayload getWriteablePayload() {
        return new WriteableCarPositionIndicatorPayload(new CarPositionIndicatorPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarPositionIndicatorPayload getReadablePayload() {
        return new ReadableCarPositionIndicatorPayload(new CarPositionIndicatorPayload());
    }

    CarPositionIndicatorPayload() {
        super(PhysicalPayload.CarPositionIndicatorEvent);

        floor = 1;
        setName("CarPositionIndicatorPayload");
    }

    public CarPositionIndicatorPayload set(int value) {
        this.floor = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        this.floor = ((CarPositionIndicatorPayload) p).floor;
    }

    @Override
    public String toString() {
        return super.toString() + "" + "(" + floor + ")";
    }

    @Override
    public Payload clone() {
        CarPositionIndicatorPayload c = new CarPositionIndicatorPayload();
        c.copyFrom(this);
        return c;
    }
}
