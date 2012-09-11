package simulator.payloads;

/**
 * Network message indicates the observed weight (in tenths of pounds) of all
 * Passengers in the Car according to the Car Weight Sensor.  Set to 0 at
 * initialization.
 *
 * @author Charles Shelton
 */
public class CarWeightPayload extends PhysicalPayload implements InternalPayload {

    /**
     * Sum of the weights of the people in the car.
     */
    private int weight;

    public static final class ReadableCarWeightPayload extends PhysicalReadablePayload {

        private final CarWeightPayload payload;

        private ReadableCarWeightPayload(CarWeightPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return current car weight in tenths of lbs
         */
        public int weight() {
            return payload.weight;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }

    }

    public static final class WriteableCarWeightPayload extends PhysicalWriteablePayload {

        private final CarWeightPayload payload;

        private WriteableCarWeightPayload(CarWeightPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return current car weight in tenths of lbs
         */
        public int weight() {
            return payload.weight;
        }

        /**
         * Set car weight
         * @param weight units of tenths of lbs
         */public void set(int weight) {
            payload.set(weight);
        }

    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarWeightPayload getWriteablePayload() {
        return new WriteableCarWeightPayload(new CarWeightPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarWeightPayload getReadablePayload() {
        return new ReadableCarWeightPayload(new CarWeightPayload());
    }


    CarWeightPayload() {
        super(PhysicalPayload.CarWeightEvent);
        setName("CarWeightPayload");
        weight = 0;
    }

    public CarWeightPayload set(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("negative weight: " + weight);
        }

        this.weight = weight;
        return this;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        weight = ((CarWeightPayload) src).weight;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append("(" + weight + ")");
        return sb.toString();
    }

    @Override
    public Payload clone() {
        CarWeightPayload c = new CarWeightPayload();
        c.copyFrom(this);
        return c;
    }
}
