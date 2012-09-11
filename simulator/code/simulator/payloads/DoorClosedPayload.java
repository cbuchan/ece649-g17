package simulator.payloads;

import simulator.framework.*;

/**
 * Indicates whether a door is fully closed.
 * @author jdevale
 */
public class DoorClosedPayload extends PhysicalPayload implements InternalPayload {

    private final Hallway hallway;
    private final Side side;
    private boolean isClosed;

    public static final class ReadableDoorClosedPayload extends PhysicalReadablePayload {

        private final DoorClosedPayload payload;

        private ReadableDoorClosedPayload(DoorClosedPayload payload) {
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
         * @return current sensor state - true if the door is closed
         */
        public boolean isClosed() {
            return payload.isClosed;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDoorClosedPayload extends PhysicalWriteablePayload {

        private final DoorClosedPayload payload;

        private WriteableDoorClosedPayload(DoorClosedPayload payload) {
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
         * @return current sensor state - true if the door is closed
         */
        public boolean isClosed() {
            return payload.isClosed;
        }

        /**
         * Set the sensor state
         * @param value true if the door is closed
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDoorClosedPayload getReadablePayload(Hallway hallway, Side side) {
        return new ReadableDoorClosedPayload(new DoorClosedPayload(hallway, side));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDoorClosedPayload getWriteablePayload(Hallway hallway, Side side) {
        return new WriteableDoorClosedPayload(new DoorClosedPayload(hallway, side));
    }

    DoorClosedPayload(Hallway hallway, Side side) {
        super(PhysicalPayload.DoorClosedEvent, ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
        isClosed = true;
        setName("DoorClosed[" + hallway + "," + side + "]");
    }

    public DoorClosedPayload set(boolean value) {
        this.isClosed = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        DoorClosedPayload c = (DoorClosedPayload) p;
        if (this.side != c.side || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        isClosed = c.isClosed;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append("(" + isClosed + ")");
        return sb.toString();
    }

    @Override
    public Payload clone() {
        DoorClosedPayload c = new DoorClosedPayload(hallway, side);
        c.copyFrom(this);
        return c;
    }
}
