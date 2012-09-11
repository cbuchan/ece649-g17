/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads;

import jSimPack.SimTime;

/**
 * Tag class for payloads that provides a read-only wrapper for payload classes.
 *
 * Any class that extends this class should methods for reading state from
 * whatever payload class they contain.
 *
 * Utility superclasses can extend the ReadablePayload to make a more specific
 * (but still abstract) readable wrapper.  See PhysicalReadablePayload in
 * PhysicalPayload for an example.
 *
 * @author Justin Ray
 */
public abstract class ReadablePayload {
    private Payload p;
    
    public ReadablePayload(Payload p) {
        this.p = p;
    }

    /**
     * This method that lets the network scheduler get the real payload class
     * internally.
     * @return the payload class.
     */
    final Payload asPayload() {
        return p;
    }

    public final int getType() {
        return p.getType();
    }

    public final int getSize() {
        return p.getSize();
    }

    public final SimTime getTimestamp() {
        return p.getTimeStamp();
    }

    public final String getName() {
        return p.getName();
    }

    /**
     * This class is implemented by each readable payload so that the call
     * to Networkable.receive() will take place within the Readable class and
     * result in delivery to the correct receive method.
     * @param networkable the object to deliver the message to.
     */
    public abstract void deliverTo(Networkable networkable);
}
