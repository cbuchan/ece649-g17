package simulator.payloads;

import simulator.framework.*;

/**
 * Internal framework message indicates how far open a door is.  If
 * <code>value</code> is 0, the door is fully closed.  If <code>value</code>
 * is 50, the door is fully open.  Initialized to 0.
 *
 * @author jdevale
 */
public class DoorPositionPayload extends PhysicalPayload implements InternalPayload {

    private final Hallway hallway;
    private final Side side;
    private double position;

    public static final class ReadableDoorPositionPayload extends PhysicalReadablePayload {

        private final DoorPositionPayload payload;

        private ReadableDoorPositionPayload(DoorPositionPayload payload) {
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
         * @return current position of the door
         */
        public double position() {
            return payload.position;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDoorPositionPayload extends PhysicalWriteablePayload {

        private final DoorPositionPayload payload;

        private WriteableDoorPositionPayload(DoorPositionPayload payload) {
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
         * @return current position of the door
         */
        public double position() {
            return payload.position;
        }

        /**
         * Set the door position
         * @param position
         */
        public void set(double position) {
            payload.set(position);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDoorPositionPayload getReadablePayload(Hallway hallway, Side side) {
        return new ReadableDoorPositionPayload(new DoorPositionPayload(hallway, side));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDoorPositionPayload getWriteablePayload(Hallway hallway, Side side) {
        return new WriteableDoorPositionPayload(new DoorPositionPayload(hallway, side));
    }

    DoorPositionPayload(Hallway hallway, Side side) {
        super(PhysicalPayload.DoorPositionEvent, ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
        position = 0;
        setName("DoorPosition[" + hallway + "," + side + "]");
    }

    public DoorPositionPayload set(double value) {
        this.position = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        DoorPositionPayload c = (DoorPositionPayload) p;
        if (this.side != c.side || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        position = c.position;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + position + ")";
    }

    @Override
    public Payload clone() {
        DoorPositionPayload c = new DoorPositionPayload(hallway, side);
        c.copyFrom(this);
        return c;
    }
}
