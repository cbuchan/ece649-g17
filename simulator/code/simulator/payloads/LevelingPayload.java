package simulator.payloads;

import simulator.framework.*;

/**
 * Indicates True when the Car is above, below, or at Floor[f,b] but close
 * enough that the Car should be traveling at slow speed to be able to stop
 * level with Floor[f,b]. (In other words, this can be thought of as a "slow
 * down" suggestion for worst case downward velocity to stop a Floor[f,b].)
 * False otherwise OR if that floor does not have a hallway exit (eg
 * - AtFloor[2,front,stop] will always be false).
 * Set to False at initialization, except the first floor exits d=Stop
 * switches are set to True at initialization.
 * 
 * @author jdevale
 * @author Kenny Stauffer
 */
public class LevelingPayload extends PhysicalPayload implements InternalPayload {

    private final Direction direction;
    private boolean value;

    public static final class ReadableLevelingPayload extends PhysicalReadablePayload {

        private final LevelingPayload payload;

        private ReadableLevelingPayload(LevelingPayload payload) {
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
         * @return the current sensor value
         */
        public boolean getValue() {
            return payload.value;
        }

        /**
         * @return the current sensor value
         */
        public boolean value() {
            return payload.value;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableLevelingPayload extends PhysicalWriteablePayload {

        private final LevelingPayload payload;

        private WriteableLevelingPayload(LevelingPayload payload) {
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
         * @return the current sensor value
         */
        public boolean getValue() {
            return payload.value;
        }

        /**
         * @return the current sensor value
         */
        public boolean value() {
            return payload.value;
        }

        /**
         *
         * @param value The sensor value to set
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableLevelingPayload getReadablePayload(Direction direction) {
        return new ReadableLevelingPayload(new LevelingPayload(direction));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableLevelingPayload getWriteablePayload(Direction direction) {
        return new WriteableLevelingPayload(new LevelingPayload(direction));
    }

    /**
     * <code>floorNumber</code> is between 1 and {@link
     * simulator.framework.Elevator#numFloors}; <code>hallway</code> is either
     * <code>Hallway.FRONT</code> or <code>Hallway.BACK</code>
     */
    LevelingPayload(Direction direction) {
        super(PhysicalPayload.LevelingEvent, ReplicationComputer.computeReplicationId(direction));
        this.direction = direction;
        value = false;
        setName("LevelingPayload" + ReplicationComputer.makeReplicationString(direction));
    }

    LevelingPayload set(boolean value) {
        this.value = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        LevelingPayload c = (LevelingPayload) p;
        if (this.direction != c.direction) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        this.value = c.value;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + value + ")";
    }

    @Override
    public Payload clone() {
        LevelingPayload c = new LevelingPayload(direction);
        c.copyFrom(this);
        return c;
    }
}
