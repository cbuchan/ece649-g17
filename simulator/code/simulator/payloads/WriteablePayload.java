/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads;

import jSimPack.SimTime;

/**
 * Tag class for payloads that provides a writeable wrapper for payload classes.
 *
 * Any class that extends this should implement get and set methods for the
 * payload object the wrapper contains.
 *
 * Utility superclasses can extend the WritablePayload to make a more specific
 * (but still abstract) writeable wrapper.  See PhysicalWritablePayload in
 * PhysicalPayload for an example.
 *
 * @author Justin Ray
 */
public abstract class WriteablePayload {
    private Payload p;

    public WriteablePayload(Payload p) {
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

    public final void setTimeStamp(SimTime timestamp) {
        p.setTimeStamp(timestamp);
    }

    public final String getName() {
        return p.getName();
    }
}
