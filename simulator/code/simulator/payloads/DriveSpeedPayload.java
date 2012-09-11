package simulator.payloads;

import simulator.framework.*;

/**
 * Indicates the current speed and direction of the drive motor.  This is the
 * actual drive status rather than the status commanded by a DrivePayload
 * framework message to the Drive Object.  (Note that there will be a time
 * delay between commanding the drive to change speed and the drive actually
 * attaining that speed. DriveSpeedPayload lets you know when the commanded
 * speed is actually attained.)
 *
 * @author jdevale
 * @author Charles Shelton
 */
public class DriveSpeedPayload extends PhysicalPayload {

    private Direction direction;
    /**
     * The absolute value of the speed of the Car, in meters per second.
     */
    private double speed;

    public static final class ReadableDriveSpeedPayload extends PhysicalReadablePayload {

        private final DriveSpeedPayload payload;

        private ReadableDriveSpeedPayload(DriveSpeedPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return The actual speed of the car, in m/s.  This is a scalar value.
         */
        public double speed() {
            return payload.speed;
        }

        /**
         * 
         * @return the actual direction the car is traveling
         */
        public Direction direction() {
            return payload.direction;
        }

        @Override
        public void deliverTo(Networkable networkable) {
            networkable.receive(this);
        }
    }

    public static final class WriteableDriveSpeedPayload extends PhysicalWriteablePayload {

        private final DriveSpeedPayload payload;

        private WriteableDriveSpeedPayload(DriveSpeedPayload payload) {
            super(payload);
            this.payload = payload;
        }

        /**
         * @return The actual speed of the car, in m/s.  This is a scalar value.
         */
        public double speed() {
            return payload.speed;
        }

        /**
         *
         * @return the actual direction the car is traveling
         */
        public Direction direction() {
            return payload.direction;
        }

        /**
         * Set the actual speed and direction of the car
         * @param direction
         * @param speed in m/s
         */
        public void set(Direction direction, double speed) {
            payload.set(direction, speed);
        }

        /**
         * Set the actual direction of the car
         * @param direction
         */
        public void setDirection(Direction direction) {
            payload.setDirection(direction);
        }

        /**
         * Set the actual speed of the car
         * @param speed in m/s
         */
        public void setSpeed(double speed) {
            payload.setSpeed(speed);
        }
    }

    /**
     * @return a WriteablePayload object for setting the system state - readable payloads of the same type
     * and replication instance will be updated with values written into this payload.
     */
    public static final WriteableDriveSpeedPayload getWriteablePayload() {
        return new WriteableDriveSpeedPayload(new DriveSpeedPayload());
    }

    /**
     * @return a ReadablePayload object for observing system state
     */
    public static final ReadableDriveSpeedPayload getReadablePayload() {
        return new ReadableDriveSpeedPayload(new DriveSpeedPayload());
    }

    DriveSpeedPayload() {
        super(PhysicalPayload.DriveSpeedEvent);
        direction = Direction.STOP;
        speed = 0;
        setName("DriveSpeedPayload");
    }

    public double speed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }

    public void set(Direction direction, double speed) {
        this.direction = direction;
        this.speed = speed;
    }

    @Override
    public void copyFrom(Payload src) {
        super.copyFrom(src);
        speed = ((DriveSpeedPayload) src).speed;
        direction = ((DriveSpeedPayload) src).direction;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());

        sb.append("(" + direction + " @ " + speed + ")");

        return sb.toString();

    }

    @Override
    public Payload clone() {
        DriveSpeedPayload c = new DriveSpeedPayload();
        c.copyFrom(this);
        return c;
    }
}
