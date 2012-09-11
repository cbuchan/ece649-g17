package simulator.payloads;

import jSimPack.SimTime;
import simulator.payloads.Payload;

public interface NetworkConnection
{
    /**
     * Registers the specified message object to be updated silently with the
     * data of any message of the same type that is sent over this network.
     */
    public abstract void registerTimeTriggered(ReadablePayload writeback);

    /**
     * Registers the specified message object to be updated with the data of
     * any message of the same type that is sent over this network, and the
     * connected component's corresponding {@link Networkable#receive} method
     * will be called.
     */
    public abstract void registerEventTriggered(ReadablePayload writeback);

    /**
     * Causes the specified message to be sent once during every subsequent
     * period of <code>period</code> <strong>microseconds</strong>.
     *
     * @param period
     * the period on which to send this message, specified in
     * <strong>microseconds</strong>
     */
    public abstract void sendTimeTriggered(WriteablePayload msg, SimTime period);

    /**
     * Enqueues the specified message for immediate broadcast.
     */
    public abstract void sendOnce(WriteablePayload msg);

    /**
     * Sets whether this connection sends and receives any messages to and
     * from the network.  If this connection is disabled,
     * the connected component will not receive any time-triggered or
     * event-triggered messages through this connection, and all messages it
     * tries to send will be silently dropped.
     */
    public abstract void setEnabled(boolean enabled);

}