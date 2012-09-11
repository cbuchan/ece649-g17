package simulator.payloads;

import simulator.framework.*;

/**
 * Framework message commands the drive motor to move the Car up and down the
 * hoistway.  Assume that Stopping from Fast speed takes a non-negligible
 * amount of time, but that Stopping when moving at Slow speed is
 * instantaneous for practical purposes. Also, note that it in general Fast
 * speed follows a velocity profile based on transitions from Fast to Slow and
 * Slow to Fast.  Set to (Stop, Stop) at initialization.
 *
 * @author jdevale
 */
public class DrivePayload extends PhysicalPayload {

    private Speed speed;
    private Direction direction;

    public static final class ReadableDrivePayload extends PhysicalReadablePayload {

        private final DrivePayload payload;

        private ReadableDrivePayload(DrivePayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * 
         * @return current speed the drive is commanded to
         */
        public Speed speed() {
            return payload.speed;
        }

        /**
         *
         * @return the current direction the drive is commanded to
         */
        public Direction direction() {
            return payload.direction;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }

    }

    public static final class WriteableDrivePayload extends PhysicalWriteablePayload {

        private final DrivePayload payload;

        private WriteableDrivePayload(DrivePayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         *
         * @return current speed the drive is commanded to
         */
        public Speed speed() {
            return payload.speed;
        }

        /**
         *
         * @return the current direction the drive is commanded to
         */
        public Direction direction() {
            return payload.direction;
        }

        /**
         * Set the speed and direction for the drive command
         * @param speed
         * @param direction
         */
        public void set(Speed speed, Direction direction) {
            payload.set(speed, direction);
        }


    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDrivePayload getWriteablePayload() {
        return new WriteableDrivePayload(new DrivePayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDrivePayload getReadablePayload() {
        return new ReadableDrivePayload(new DrivePayload());
    }


    DrivePayload() {
        super(PhysicalPayload.DriveEvent);
        direction = Direction.STOP;
        speed = Speed.STOP;
        setName("DrivePayload");
    }

    public void set(Speed speed, Direction direction) {
        this.speed = speed;
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }

    public Speed speed() {
        return speed;
    }

    public Direction direction(Direction direction) {
        this.direction = direction;
        return direction;
    }

    public Speed speed(Speed speed) {
        this.speed = speed;
        return speed;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        speed = ((DrivePayload) src).speed;
        direction = ((DrivePayload) src).direction;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + direction + "," + speed + ")";
    }

    @Override
    public Payload clone() {
        DrivePayload c = new DrivePayload();
        c.copyFrom(this);
        return c;
    }
}
