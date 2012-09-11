package simulator.payloads;

import simulator.framework.*;

/**
 * Framework message indicates whether a door reversal switch has been
 * triggered.  Initialized to <code>false</code>.
 *
 * @author jdevale
 */
public class DoorReversalPayload extends PhysicalPayload implements InternalPayload {

    private final Hallway hallway;
    private final Side side;
    private boolean isReversing;

    public static final class ReadableDoorReversalPayload extends PhysicalReadablePayload {

        private final DoorReversalPayload payload;

        private ReadableDoorReversalPayload(DoorReversalPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return hall location of this instance
         */
        public Hallway getHallway() {
            return payload.hallway;
        }

        /**
         * @return which Side this instance is on
         */
        public Side getSide() {
            return payload.side;
        }

        /**
         *
         * @return current sensor state - true if the reversal is triggered
         */
        public boolean isReversing() {
            return payload.isReversing;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDoorReversalPayload extends PhysicalWriteablePayload {

        private final DoorReversalPayload payload;

        private WriteableDoorReversalPayload(DoorReversalPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return hall location of this instance
         */
        public Hallway getHallway() {
            return payload.hallway;
        }

        /**
         * @return which Side this instance is on
         */
        public Side getSide() {
            return payload.side;
        }

        /**
         * 
         * @return current sensor state - true if the reversal is triggered
         */
        public boolean isReversing() {
            return payload.isReversing;
        }

        /**
         * Set the sensor value
         * @param value true if reversal is occurring
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDoorReversalPayload getReadablePayload(Hallway hallway, Side side) {
        return new ReadableDoorReversalPayload(new DoorReversalPayload(hallway, side));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDoorReversalPayload getWriteablePayload(Hallway hallway, Side side) {
        return new WriteableDoorReversalPayload(new DoorReversalPayload(hallway, side));
    }

    DoorReversalPayload(Hallway hallway, Side side) {
        super(PhysicalPayload.DoorReversalEvent, ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
        isReversing = false;

        setName("DoorReversal[" + hallway + "," + side + "]");
    }

    public DoorReversalPayload set(boolean value) {
        this.isReversing = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        DoorReversalPayload c = (DoorReversalPayload) p;
        if (this.side != c.side || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        isReversing = c.isReversing;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + isReversing + ")";
    }

    @Override
    public Payload clone() {
        DoorReversalPayload c = new DoorReversalPayload(hallway, side);
        c.copyFrom(this);
        return c;
    }
}
