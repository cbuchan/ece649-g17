package simulator.payloads;

import simulator.framework.*;

/**
 * Framework message sent by a controller is a command to the light actuator
 * to turn the light on or off; framework message from the light indicates
 * whether the light is on or off.
 *
 * @author jdevale
 */
public class HallLightPayload extends PhysicalPayload {

    private final int floor;
    private final Hallway hallway;
    private final Direction direction;
    private boolean lighted;

    public static final class ReadableHallLightPayload extends PhysicalReadablePayload {

        private final HallLightPayload payload;

        private ReadableHallLightPayload(HallLightPayload payload) {
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

    public static final class WriteableHallLightPayload extends PhysicalWriteablePayload {

        private final HallLightPayload payload;

        private WriteableHallLightPayload(HallLightPayload payload) {
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
         * @return current state of the light - true if lighted
         */
        public boolean lighted() {
            return payload.lighted;
        }

        /**
         * Set the light state
         * @param lighted true if lighted
         */
        public void set(boolean lighted) {
            payload.set(lighted);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableHallLightPayload getWriteablePayload(int floor, Hallway hallway, Direction direction) {
        return new WriteableHallLightPayload(new HallLightPayload(floor, hallway, direction));
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableHallLightPayload getReadablePayload(int floor, Hallway hallway, Direction direction) {
        return new ReadableHallLightPayload(new HallLightPayload(floor, hallway, direction));
    }

    HallLightPayload(HallLightPayload p) {
        super(p);
        this.floor = p.floor;
        this.hallway = p.hallway;
        this.direction = p.direction;
        this.lighted = p.lighted;
    }

    public HallLightPayload(int floor, Hallway hallway, Direction direction) {
        super(PhysicalPayload.HallLightEvent, ReplicationComputer.computeReplicationId(floor, hallway, direction));
        this.floor = floor;
        this.hallway = hallway;
        this.direction = direction;
        lighted = false;

        setName("HallLightPayload[" + floor + "," + hallway + "," + direction + "]");
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        HallLightPayload c = (HallLightPayload) p;
        if (this.floor != c.floor || this.hallway != c.hallway || this.direction != c.direction) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        lighted = c.lighted;
    }

    public HallLightPayload set(boolean lampOn) {
        this.lighted = lampOn;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + lighted + ")";
    }

    @Override
    public Payload clone() {
        return new HallLightPayload(this);
    }
}
