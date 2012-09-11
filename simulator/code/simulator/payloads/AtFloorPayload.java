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
public class AtFloorPayload extends PhysicalPayload implements InternalPayload {

    private final int floor;
    private final Hallway hallway;
    private boolean value;

    public static final class ReadableAtFloorPayload extends PhysicalReadablePayload {

        private final AtFloorPayload payload;

        private ReadableAtFloorPayload(AtFloorPayload payload) {
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

    public static final class WriteableAtFloorPayload extends PhysicalWriteablePayload {

        private final AtFloorPayload payload;

        private WriteableAtFloorPayload(AtFloorPayload payload) {
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
         * @param value sensor value to set
         */
        public void set(boolean value) {
            payload.set(value);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableAtFloorPayload getReadablePayload(int floor, Hallway hallway) {
        return new ReadableAtFloorPayload(new AtFloorPayload(floor, hallway));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableAtFloorPayload getWriteablePayload(int floor, Hallway hallway) {
        return new WriteableAtFloorPayload(new AtFloorPayload(floor, hallway));
    }

    /**
     * <code>floorNumber</code> is between 1 and {@link
     * simulator.framework.Elevator#numFloors}; <code>hallway</code> is either
     * <code>Hallway.FRONT</code> or <code>Hallway.BACK</code>
     */
    AtFloorPayload(int floorNumber, Hallway hallway) {
        super(PhysicalPayload.AtFloorEvent, ReplicationComputer.computeReplicationId(floorNumber, hallway));
        this.floor = floorNumber;
        this.hallway = hallway;
        value = false;
        setName("AtFloorPayload[" + floorNumber + "," + hallway + "]");
    }

    AtFloorPayload set(boolean value) {
        this.value = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        AtFloorPayload c = (AtFloorPayload) p;
        if (this.floor != c.floor || this.hallway != c.hallway) {
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
        AtFloorPayload c = new AtFloorPayload(floor, hallway);
        c.copyFrom(this);
        return c;
    }
}
