package simulator.payloads;

import simulator.framework.*;

/**
 * Network message indicates whether a hall call has been requested at
 * Hallway[f,b] for direction d, and a framework message indicates whether the
 * button is currently held down.  Set to False at initialization in both
 * cases.
 *
 * @author jdevale
 */
public class HallCallPayload extends PhysicalPayload {

    private final int floor;
    private final Hallway hallway;
    private final Direction direction;
    private boolean pressed;

    public static final class ReadableHallCallPayload extends PhysicalReadablePayload {

        private final HallCallPayload payload;

        private ReadableHallCallPayload(HallCallPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return floor location of this instance
         */
        public int getFloor() {
            return payload.floor;
        }

        /**
         * @return hall location of this instance
         */
        public Hallway getHallway() {
            return payload.hallway;
        }

        /**
         * @return direction of this instance
         */
        public Direction getDirection() {
            return payload.direction;
        }

        /**
         * 
         * @return current button state - true if pressed.
         */
        public boolean pressed() {
            return payload.pressed;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableHallCallPayload extends PhysicalWriteablePayload {

        private final HallCallPayload payload;

        private WriteableHallCallPayload(HallCallPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return floor location of this instance
         */
        public int getFloor() {
            return payload.floor;
        }

        /**
         * @return hall location of this instance
         */
        public Hallway getHallway() {
            return payload.hallway;
        }

        /**
         * @return direction of this instance
         */
        public Direction getDirection() {
            return payload.direction;
        }

        /**
         *
         * @return current button state - true if pressed.
         */
        public boolean pressed() {
            return payload.pressed;
        }

        /**
         * Set the button state
         * @param pressed true if pressed
         */
        public void set(boolean pressed) {
            payload.set(pressed);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableHallCallPayload getWriteablePayload(int floor, Hallway hallway, Direction direction) {
        return new WriteableHallCallPayload(new HallCallPayload(floor, hallway, direction));
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableHallCallPayload getReadablePayload(int floor, Hallway hallway, Direction direction) {
        return new ReadableHallCallPayload(new HallCallPayload(floor, hallway, direction));
    }

    HallCallPayload(int floor, Hallway hallway, Direction direction) {
        super(PhysicalPayload.HallCallEvent, ReplicationComputer.computeReplicationId(floor, hallway, direction));
        this.floor = floor;
        this.hallway = hallway;
        this.direction = direction;
        pressed = false;

        setName("HallCallPayload[" + floor + "," + hallway + "," + direction + "]");
    }

    public HallCallPayload set(boolean pressed) {
        this.pressed = pressed;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        HallCallPayload c = (HallCallPayload) p;
        if (this.floor != c.floor || this.hallway != c.hallway || this.direction != c.direction) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        pressed = c.pressed;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append("(" + pressed + ")");
        return sb.toString();
    }

    @Override
    public Payload clone() {
        HallCallPayload c = new HallCallPayload(floor, hallway, direction);
        c.copyFrom(this);
        return c;
    }
}
