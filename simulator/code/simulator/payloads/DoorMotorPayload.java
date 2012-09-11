package simulator.payloads;

import simulator.framework.*;

/**
 * Framework message that commands the doors to open, close, nudge, or stop moving.
 * Set to Stop at initialization.
 *
 * @author jdevale
 */
public class DoorMotorPayload extends PhysicalPayload {

    private final Hallway hallway;
    private final Side side;
    private DoorCommand command;

    public static final class ReadableDoorMotorPayload extends PhysicalReadablePayload {

        private final DoorMotorPayload payload;

        private ReadableDoorMotorPayload(DoorMotorPayload payload) {
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
         * @return the current DoorCommand being given to the door.
         */
        public DoorCommand command() {
            return payload.command;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDoorMotorPayload extends PhysicalWriteablePayload {

        private final DoorMotorPayload payload;

        private WriteableDoorMotorPayload(DoorMotorPayload payload) {
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
         * @return the current DoorCommand being given to the door.
         */
        public DoorCommand command() {
            return payload.command;
        }

        /**
         * Set the command given to the door.
         * @param command
         */
        public void set(DoorCommand command) {
            payload.set(command);
        }
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDoorMotorPayload getReadablePayload(Hallway hallway, Side side) {
        return new ReadableDoorMotorPayload(new DoorMotorPayload(hallway, side));
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDoorMotorPayload getWriteablePayload(Hallway hallway, Side side) {
        return new WriteableDoorMotorPayload(new DoorMotorPayload(hallway, side));
    }

    DoorMotorPayload(Hallway hallway, Side side) {
        super(PhysicalPayload.DoorMotorEvent, ReplicationComputer.computeReplicationId(hallway, side));
        if (hallway != Hallway.BACK && hallway != Hallway.FRONT) {
            throw new IllegalArgumentException("hallway should be FRONT or BACK");
        }
        if (side != Side.LEFT && side != Side.RIGHT) {
            throw new IllegalArgumentException("side should be LEFT or RIGHT");
        }

        this.hallway = hallway;
        this.side = side;
        command = DoorCommand.STOP;

        setName("DoorMotorPayload[" + hallway + "," + side + "]");
    }

    public DoorMotorPayload set(DoorCommand value) {
        this.command = value;
        return this;
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        DoorMotorPayload c = (DoorMotorPayload) p;
        if (this.side != c.side || this.hallway != c.hallway) {
            throw new RuntimeException("Wrong replication instance of " + this);
        }
        command = c.command;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + command + ")";
    }

    @Override
    public Payload clone() {
        DoorMotorPayload c = new DoorMotorPayload(hallway, side);
        c.copyFrom(this);
        return c;
    }
}
