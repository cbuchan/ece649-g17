package simulator.payloads;

import simulator.framework.*;

/**
 * Network messages indicate whether a car call has been requested for
 * Hallway[f,b], and framework messages indicate whether the button is
 * currently held down.  Set to False at initialization in both cases.
 *
 * @author jdevale
 */
public class CarCallPayload extends PhysicalPayload {

    private final int floor;
    private final Hallway hallway;
    private boolean pressed;

    public static final class ReadableCarCallPayload extends PhysicalReadablePayload {

        private final CarCallPayload payload;

        private ReadableCarCallPayload(CarCallPayload payload) {
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
         * @return the current state of the button
         */
        public boolean isPressed() {
            return payload.pressed;
        }

        /**
         * @return the current state of the button
         */
        public boolean pressed() {
            return payload.pressed;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableCarCallPayload extends PhysicalWriteablePayload {

        private final CarCallPayload payload;

        private WriteableCarCallPayload(CarCallPayload payload) {
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
         * @return the current state of the button
         */
        public boolean pressed() {
            return payload.pressed;
        }

        /**
         * Set the button state
         * @param pressed true if the button is pressed
         */
        public void set(boolean pressed) {
            payload.set(pressed);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableCarCallPayload getReadablePayload(int floor, Hallway hallway) {
        return new ReadableCarCallPayload(new CarCallPayload(floor, hallway));
    }

    /**
     * @return a WriteblePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableCarCallPayload getWriteablePayload(int floor, Hallway hallway) {
        return new WriteableCarCallPayload(new CarCallPayload(floor, hallway));
    }

    /**
     * <code>floorNumber</code> is between 1 and {@link
     * simulator.framework.Elevator#numFloors}; <code>hallway</code> is either
     * <code>Hallway.FRONT</code> or <code>Hallway.BACK</code>
     */
    CarCallPayload(int floor, Hallway hallway) {
        super(PhysicalPayload.CarCallEvent, ReplicationComputer.computeReplicationId(floor, hallway));
        this.floor = floor;
        this.hallway = hallway;
        this.pressed = false;
        setName("CarCallPayload(" + floor + ", " + hallway + ")");

    }

    CarCallPayload set(boolean buttonValue) {
        this.pressed = buttonValue;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        CarCallPayload c = (CarCallPayload) p;
        if (this.floor != c.floor || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        pressed = c.pressed;
    }

    /**
     * Returns a String that represents the value of this object.
     * @return a string representation of the receiver
     */
    @Override
    public String toString() {
        return super.toString() + "(" + pressed + ")";
    }

    @Override
    public Payload clone() {
        CarCallPayload c = new CarCallPayload(floor, hallway);
        c.copyFrom(this);
        return c;
    }
}
