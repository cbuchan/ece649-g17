package simulator.payloads;

import jSimPack.SimTime;

/**
 * A message sent between components in the elevator.
 *
 * Different objects keep a local Payload object that is registered with the
 * Physical or CAN network as a sender or receiver.  The values in the payload
 * class are copied from sender objects to receiver objects by the scheduler at
 * the appropriate time.
 *
 * It is important that objects that represent the same state value or message
 * have the same type.  This is how the scheduler knows which objects to copy.
 *
 */
public abstract class Payload implements Comparable<Payload>, Cloneable {

    private int type;
    private SimTime timeStamp;
    private String name;



    Payload(Payload p) {
        this.type = p.type;
        this.timeStamp = p.timeStamp;
        this.name = p.name;
    }

    Payload(int type) {
        this.type = type;
    }

    @Override
    public abstract Payload clone();

    public int getType() {
        return type;
    }

    @Override
    public final int hashCode() {
        return type;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Payload) {
            return ((Payload) obj).type == this.type;
        }
        return false;
    }

    public int compareTo(Payload mp) {
        if (this.type < mp.type) {
            return -1;
        }
        if (this.type > mp.type) {
            return 1;
        }
        return 0;
    }

    public final SimTime getTimeStamp() {
        return timeStamp;
    }

    public final void setTimeStamp(SimTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    abstract public int getSize();

    public final String getName() {
        return name;
    }

    protected final void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Copies the state of the given object into this object's state.
     * Subclasses that override this method should also call this
     * implementation.
     *
     * @param p the object whose state should be copied into this object
     */
    public void copyFrom(Payload p) {
        this.type = p.type;
        this.timeStamp = p.timeStamp;
        this.name = p.name;
    }
}
