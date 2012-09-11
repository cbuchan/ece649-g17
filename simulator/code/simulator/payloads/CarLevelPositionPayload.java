package simulator.payloads;

/**
 * Reports approximate position of car in hoistway based on position sensors
 * placed at 10 cm intervals in the hoistway.  Gets updated each time the car
 * passes one of these sensors.  Set to 0 position at initialization
 *
 * @author Charles Shelton
 */
public class CarLevelPositionPayload extends PhysicalPayload {

    /**
     * The location of the car within the hoistway, in millimeters from the
     * level position of the first floor.
     */
    private int position;

    public static final class ReadableCarLevelPositionPayload extends PhysicalReadablePayload {

        private final CarLevelPositionPayload payload;

        private ReadableCarLevelPositionPayload(CarLevelPositionPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return  The location of the last sensor that the car passed.  Sensors
         * are 10cm apart.  Value is in units of mm.
         */
        public int position() {
            return payload.position;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarLevelPositionPayload extends PhysicalWriteablePayload {

        private final CarLevelPositionPayload payload;

        private WriteableCarLevelPositionPayload(CarLevelPositionPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return  The location of the last sensor that the car passed.  Sensors
         * are 10cm apart.  Value is in units of mm.
         */
        public int position() {
            return payload.position;
        }

        /**
         * Set the current position 
         * @param position position of car in mm.
         */
        public void set(int position) {
            payload.set(position);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarLevelPositionPayload getWriteablePayload() {
        return new WriteableCarLevelPositionPayload(new CarLevelPositionPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarLevelPositionPayload getReadablePayload() {
        return new ReadableCarLevelPositionPayload(new CarLevelPositionPayload());
    }

    CarLevelPositionPayload() {
        super(PhysicalPayload.CarLevelPositionEvent);
        position = 0;
        setName("CarLevelPositionPayload");
    }

    public void set(int position) {
        this.position = position;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        this.position = ((CarLevelPositionPayload) p).position;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + position + ")";
    }

    @Override
    public Payload clone() {
        CarLevelPositionPayload c = new CarLevelPositionPayload();
        c.copyFrom(this);
        return c;
    }
}
