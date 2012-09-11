package simulator.payloads;

import simulator.framework.*;

/**
 * A HoistwayLimit[d] switch activates when the car has over-run the hoistway
 * limits and is in an emergency stopping situation. The d=Up switch is at top
 * of hoistway; d=Down switch is at bottom of hoistway.  Set to
 * <code>false</code> at initialization.
 *
 * @author jdevale
 */
public class HoistwayLimitPayload extends PhysicalPayload implements InternalPayload {

    private final Direction direction;
    private boolean exceeded;

    public static final class ReadableHoistwayLimitPayload extends PhysicalReadablePayload {

        private final HoistwayLimitPayload payload;

        private ReadableHoistwayLimitPayload(HoistwayLimitPayload payload) {
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
         *
         * @return the current sensor value - true if the hoistway limit was exceeded
         */
        public boolean exceeded() {
            return payload.exceeded;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableHoistwayLimitPayload extends PhysicalWriteablePayload {

        private final HoistwayLimitPayload payload;

        private WriteableHoistwayLimitPayload(HoistwayLimitPayload payload) {
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
         * 
         * @return the current sensor value - true if the hoistway limit was exceeded
         */
        public boolean exceeded() {
            return payload.exceeded;
        }

        /**
         * Set the sensor value
         * @param value true if exceeded
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableHoistwayLimitPayload getWriteablePayload(Direction direction) {
        return new WriteableHoistwayLimitPayload(new HoistwayLimitPayload(direction));
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableHoistwayLimitPayload getReadablePayload(Direction direction) {
        return new ReadableHoistwayLimitPayload(new HoistwayLimitPayload(direction));
    }

    HoistwayLimitPayload(Direction direction) {
        super(PhysicalPayload.HoistwayLimitEvent, ReplicationComputer.computeReplicationId(direction));
        this.direction = direction;
        exceeded = false;

        setName("HoistwayLimit[" + direction + "]");
    }

    public HoistwayLimitPayload set(boolean value) {
        this.exceeded = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        HoistwayLimitPayload c = (HoistwayLimitPayload) p;
        if (this.direction != c.direction) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        exceeded = c.exceeded;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + exceeded + ")";
    }

    @Override
    public Payload clone() {
        HoistwayLimitPayload c = new HoistwayLimitPayload(direction);
        c.copyFrom(this);
        return c;
    }
}
