package simulator.payloads;

import simulator.framework.*;

/**
 * Indicates whether a door is fully open.  Initialized to <code>false</code>.
 *
 * @author jdevale
 */
public class DoorOpenPayload extends PhysicalPayload implements InternalPayload {

    private final Hallway hallway;
    private final Side side;
    private boolean isOpen;

    public static final class ReadableDoorOpenPayload extends PhysicalReadablePayload {

        private final DoorOpenPayload payload;

        private ReadableDoorOpenPayload(DoorOpenPayload payload) {
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
         * @return current sensor state - true if the door is fully open
         */
        public boolean isOpen() {
            return payload.isOpen;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDoorOpenPayload extends PhysicalWriteablePayload {

        private final DoorOpenPayload payload;

        private WriteableDoorOpenPayload(DoorOpenPayload payload) {
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
         * @return current sensor state - true if the door is fully open
         */
        public boolean isOpen() {
            return payload.isOpen;
        }

        /**
         * Set the sensor state
         * @param value true if the door is fully open
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDoorOpenPayload getReadablePayload(Hallway hallway, Side side) {
        return new ReadableDoorOpenPayload(new DoorOpenPayload(hallway, side));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDoorOpenPayload getWriteablePayload(Hallway hallway, Side side) {
        return new WriteableDoorOpenPayload(new DoorOpenPayload(hallway, side));
    }

    DoorOpenPayload(Hallway hallway, Side side) {
        super(PhysicalPayload.DoorOpenedEvent, ReplicationComputer.computeReplicationId(hallway, side));
        this.hallway = hallway;
        this.side = side;
        isOpen = false;
        setName("DoorOpened[" + hallway + "," + side + "]");
    }

    public DoorOpenPayload set(boolean value) {
        this.isOpen = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        DoorOpenPayload c = (DoorOpenPayload) p;
        if (this.side != c.side || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        isOpen = c.isOpen;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + isOpen + ")";
    }

    @Override
    public Payload clone() {
        DoorOpenPayload c = new DoorOpenPayload(hallway, side);
        c.copyFrom(this);
        return c;
    }
}
