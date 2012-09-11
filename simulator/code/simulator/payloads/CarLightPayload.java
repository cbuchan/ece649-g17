package simulator.payloads;

import simulator.framework.*;

/**
 * Framework message sent by a controller is a command to the light actuator
 * to turn the light on or off; framework message from the light indicates
 * whether the light is on or off.
 *  
 * @author jdevale
 */
public class CarLightPayload extends PhysicalPayload {

    private final int floor;
    private final Hallway hallway;
    private boolean lighted;

    public static final class ReadableCarLightPayload extends PhysicalReadablePayload {

        private final CarLightPayload payload;

        private ReadableCarLightPayload(CarLightPayload payload) {
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
         * @return the current light value
         */
        public boolean isLighted() {
            return payload.lighted;
        }

        /**
         * @return the current light value
         */
        public boolean lighted() {
            return payload.lighted;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarLightPayload extends PhysicalWriteablePayload {

        private final CarLightPayload payload;

        private WriteableCarLightPayload(CarLightPayload payload) {
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
         * @return the current light value
         */
        public boolean isLighted() {
            return payload.lighted;
        }

        /**
         * @return the current light value
         */
        public boolean lighted() {
            return payload.lighted;
        }

        /**
         *
         * @param lighted sensor value to set
         */
        public void set(boolean lighted) {
            payload.set(lighted);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarLightPayload getReadablePayload(int floor, Hallway hallway) {
        return new ReadableCarLightPayload(new CarLightPayload(floor, hallway));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarLightPayload getWriteablePayload(int floor, Hallway hallway) {
        return new WriteableCarLightPayload(new CarLightPayload(floor, hallway));
    }

    CarLightPayload(int floor, Hallway hallway) {
        super(PhysicalPayload.CarLightEvent, ReplicationComputer.computeReplicationId(floor, hallway));
        this.floor = floor;
        this.hallway = hallway;
        lighted = false;

        setName("CarLightPayload[" + floor + ", " + hallway + "]");
    }

    CarLightPayload set(boolean value) {
        this.lighted = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        CarLightPayload c = (CarLightPayload) p;
        if (this.floor != c.floor || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        lighted = c.lighted;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + lighted + ")";
    }

    @Override
    public Payload clone() {
        CarLightPayload c = new CarLightPayload(floor, hallway);
        c.copyFrom(this);
        return c;
    }
}
