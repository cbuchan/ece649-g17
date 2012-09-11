package simulator.payloads;

import simulator.framework.*;

/**
 * Indicates whether the Up/Down arrows placed on the car doorframes are
 * illuminated. Used by Passengers on a Floor to figure out whether to enter
 * the Car.  Set to Off at initialization.
 *
 * @author jdevale
 */
public class CarLanternPayload extends PhysicalPayload {

    /**
     * Which lantern this message represents.
     */
    private final Direction direction;
    /**
     * Whether this lantern is illuminated.
     */
    private boolean lighted;

    public static final class ReadableCarLanternPayload extends PhysicalReadablePayload {

        private final CarLanternPayload payload;

        private ReadableCarLanternPayload(CarLanternPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return direction of this instance
         */
        public Direction getDirection() {
            return payload.direction;
        }

        /**
         * @return current state of the light - true if lighted
         */
        public boolean lighted() {
            return payload.lighted;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarLanternPayload extends PhysicalWriteablePayload {

        private final CarLanternPayload payload;

        private WriteableCarLanternPayload(CarLanternPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return direction of this instance
         */
        public Direction getDirection() {
            return payload.direction;
        }

        /**
         * @return current state of the light - true if lighted
         */
        public boolean lighted() {
            return payload.lighted;
        }

        /**
         * Set the current state of the light
         * @param value True if lighted
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a WriteblePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarLanternPayload getWriteablePayload(Direction direction) {
        return new WriteableCarLanternPayload(new CarLanternPayload(direction));
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarLanternPayload getReadablePayload(Direction direction) {
        return new ReadableCarLanternPayload(new CarLanternPayload(direction));
    }

    /**
     * <code>direction</code> is <code>Direction.UP</code> or
     * <code>Direction.DOWN</code>
     */
    CarLanternPayload(Direction direction) {
        super(PhysicalPayload.CarLanternEvent, ReplicationComputer.computeReplicationId(direction));
        if (direction != Direction.UP && direction != Direction.DOWN) {
            throw new IllegalArgumentException("CarLantern.direction must be UP or DOWN.");
        }

        this.direction = direction;
        this.lighted = false;

        setName("CarLanternPayload[" + direction + "]");

    }

    CarLanternPayload set(boolean value) {
        this.lighted = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        CarLanternPayload c = (CarLanternPayload) p;
        if (this.direction != c.direction) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        lighted = c.lighted;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append("(" + lighted + ")");
        return sb.toString();
    }

    @Override
    public Payload clone() {
        CarLanternPayload c = new CarLanternPayload(direction);
        c.copyFrom(this);
        return c;
    }
}
