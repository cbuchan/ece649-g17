package simulator.payloads;

import simulator.framework.faultmodels.NetworkFaultModel;
import jSimPack.SimTime;

import java.util.*;
import simulator.framework.Harness;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;

/**
 * A simple container that holds two objects.  This class is useful as the
 * element type for an array or {@link java.util.Collection}, or as the value
 * type in a {@link java.util.Map}, when two objects of possibly different
 * types need to be associated with each other as a single logical "entry".
 */
class Pair<T1, T2> {

    final T1 first;
    final T2 second;

    Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}

/**
 * A generic computer network that transmits "high priority" messages before
 * "low priority" messages. High priority messages are less than low priority
 * messages according to their <code>compareTo</code> method. Both
 * event-triggered and time-triggered facilities are provided for sending and
 * receiving messages. Network components that have only time-triggered
 * message behavior can simply call {@link #getConnection()} to acquire
 * a connection to this network. Components that want to perform
 * event-triggered behavior must implement {@link Networkable} and call {@link
 * #getConnection(Networkable)}.
 *
 * The network connections and adapters have been modified so that send methods
 * only accept writeable payloads, and receive methods only accept readable payloads.
 * Readable and Writeable objects are wrapper classes that enclose a payload object
 * but are not directly descended from it.
 * 
 * @author Christopher Martin
 * @author Beth Latronico
 * @author Nick Zamora
 * @author Kenny Stauffer
 * @author Justin Ray
 */
public abstract class NetworkScheduler implements TimeSensitive {

    /**
     * Allows elevator components to send and receive messages through the
     * network.
     */
    public class Connection implements NetworkConnection {

        /**
         * A task that periodically enqueues a specified message for
         * transmission.
         */
        protected class RepeatedPayload implements TimeSensitive {

            private WriteablePayload message;
            private final SimTime period;
            private final Timer timer;
            private SimTime deadline;

            /**
             * Creates a new task that will enqueue the specified message once
             * every period.
             */
            RepeatedPayload(WriteablePayload message, SimTime period) {
                this.message = message;
                this.period = period;
                timer = new Timer(this);
                log(this, ": created");
            }

            /**
             * Enqueus the message for transmission, and will repeatedly
             * enqueue after every period.
             */
            public void start() {
                log(this, ": start()");
                deadline = SimTime.add(Harness.getTime(), period);
                timer.start(period);
                sendOnce(message);
            }

            public void timerExpired(Object callback) {
                log(this, ": timerExpired(", callback, ") last sent=", message.asPayload().getTimeStamp(),
                        " deadline=", deadline);
                if ((message.asPayload().getTimeStamp() == null || message.asPayload().getTimeStamp().isAfter(deadline))) {
                    Harness.log("RepeatedPayload", toString(),"failed to meet deadline.  LastTimstamp=", message.asPayload().getTimeStamp());
                    //only throw exceptions in the fault-free case
                    //System.err.println("Fault Models: " + faultModels);
                    //if (faultModels.isEmpty()) {
                      //throw new RuntimeException(toString() + ": failed to meet deadline.");
                    //}
                    //Fixed the timestamp handling when messages are dropped so that
                    //this exception should only be thrown if the schedule is not being met
                    throw new RuntimeException(toString() + ": failed to meet deadline.");
                }
                deadline = SimTime.add(Harness.getTime(), period);
                log(this, ": next deadline is:", deadline);
                timer.start(period);
                sendOnce(message);
            }

            @Override
            public String toString() {
                return "RepeatedPayload[msg=" + message.toString() + ",period=" + period + "]";
            }
        }
        private boolean enabled = true;
        private final Networkable networkNode;

        public Connection() {
            networkNode = null;
        }

        public Connection(Networkable networkNode) {
            this.networkNode = networkNode;
            log(this, ": created");
        }

        /**
         * Registers the specified message object to be updated silently with
         * the data of any message of the same type that is sent over this
         * network.
         */
        public void registerTimeTriggered(ReadablePayload writeback) {
            Payload payload = writeback.asPayload();
            //log(toString()+": registerTimeTriggered("+writeback+")");
            if (payload == null) {
                throw new NullPointerException("writeback");
            }
            if (verbose) {
                log(this, ": registerTimeTriggered(", payload, ")");
            }
            Collection<Payload> c = listeners.get(payload);
            if (c == null) {
                c = new ArrayList<Payload>();
                listeners.put(payload, c);
            }
            c.add(payload);
        }

        /**
         * Registers the specified message object to be updated with the data of
         * any message of the same type that is sent over this network, and the
         * connected component's corresponding {@link Networkable#receive}
         * method will be called.
         */
        public void registerEventTriggered(ReadablePayload rpayload) {
            Payload payload = rpayload.asPayload();
            //log(toString()+": registerEventTriggered("+writeback+")");
            if (payload == null) {
                throw new NullPointerException("writeback");
            }
            if (networkNode == null) {
                throw new IllegalStateException(this + ": connection was created for" + " time triggered messages only");
            }
            if (verbose) {
                log(this, ": registerEventTriggered(", payload, ")");
            }
            registerTimeTriggered(rpayload);
            Collection<Pair<Networkable, ReadablePayload>> c = receivers.get(payload);
            if (c == null) {
                c = new ArrayList<Pair<Networkable, ReadablePayload>>();
                receivers.put(payload, c);
            }
            c.add(new Pair<Networkable, ReadablePayload>(networkNode, rpayload));
        }

        /**
         * Causes the specified message to be sent once during every subsequent
         * period of <code>period</code> <strong>microseconds</strong>.
         * 
         * @param period
         *            the period on which to send this message, specified in
         *            <strong>microseconds</strong>
         */
        public void sendTimeTriggered(WriteablePayload wpayload, SimTime period) {
            Payload payload = wpayload.asPayload();
//            log(toString() + ": sendTimeTriggered("+message+", "+period+")");
            if (payload == null) {
                throw new NullPointerException("msg");
            }
            if (period == null) {
                throw new NullPointerException("period");
            }
            if (!period.isPositive()) {
                throw new IllegalArgumentException("period: " + period);
            }
            //check to see if there is already a repeatedPayload for the sender
            if (periodicSenders.containsKey(payload)) {
                //if so, overwrite it with the new payload information.  
                RepeatedPayload rp = periodicSenders.get(payload);
                if (!rp.period.equals(period)) {
                    throw new RuntimeException("Cannot send message " + payload + " with a new period.  The period must not change once the sender is registered");
                }
                log(this, ": sendTimeTriggered(", payload, ", ", period, ")");
                log(this, "       replacing old message ", rp.message.asPayload());
                //System.out.println("Replacing " + rp.message + " with " + message);
                //copy the timestamp from the old object so that the deadline will still work
                payload.setTimeStamp(rp.message.asPayload().getTimeStamp());
                //use the new message object
                rp.message = wpayload;

            } else {
                //otherwise make a new RepeatedPayload
                //System.out.println("New Message:" + message);
                log(this, ": sendTimeTriggered(", payload, ", ", period, ")");
                RepeatedPayload rp = new RepeatedPayload(wpayload, period);
                periodicSenders.put(payload, rp);
                rp.start();
            }

        }

        /**
         * Enqueues the specified message for broadcast.
         */
        public void sendOnce(WriteablePayload message) {
            Payload payload = message.asPayload();
            if (verbose) {
                log(this, ": sendOnce(", payload, ")");
            }
            if (payload == null) {
                throw new NullPointerException("msg");
            }
            if (!enabled) {
                return;
            }

            enqueueMessage(payload);
        }

        /**
         * Sets whether this connection sends and receives any messages to and
         * from the network. If this connection is disabled, the connected
         * component will not receive any time-triggered or event-triggered
         * messages through this connection, and all messages it tries to send
         * will be silently dropped.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return "Connection#" + networkNode;
        }
    }
    protected boolean verbose = false;
    protected boolean dropVerbose = false;
    private Timer timer;
    private Queue<Payload> outgoingMessages;
    private Map<Payload, Collection<Payload>> listeners = new HashMap<Payload, Collection<Payload>>();
    private Map<Payload, Collection<Pair<Networkable, ReadablePayload>>> receivers = new HashMap<Payload, Collection<Pair<Networkable, ReadablePayload>>>();
    private Map<Payload, Connection.RepeatedPayload> periodicSenders = new HashMap<Payload, Connection.RepeatedPayload>();
    private ArrayList<NetworkFaultModel> faultModels = new ArrayList<NetworkFaultModel>();
    private SimTime bitWidth;
    /**
     * The message that is currently being transmitted across the network.
     */
    private Payload currentMessage;
    private NetworkUtilization utilization;   

    public NetworkScheduler() {
        this(SimTime.ZERO);
    }

    public NetworkScheduler(SimTime bitWidth) {
        if (bitWidth.isNegative()) {
            throw new IllegalArgumentException(
                    "bitWidth: " + bitWidth + " is not positive");
        }
        log(toString(), "bits width=", bitWidth);
        this.bitWidth = bitWidth;
        timer = new Timer(this);
        outgoingMessages = new PriorityQueue<Payload>();
        if (bitWidth.equals(SimTime.ZERO)) {
            //disable utilization
            utilization = new NetworkUtilization(toString() + " Utilization", false);
        } else {
            utilization = new NetworkUtilization(toString() + " Utilization", true);
        }
    }

    public SimTime getBitWidth() {
        return bitWidth;
    }

    protected void enqueueMessage(Payload message) {
        log("enqueueMessage(", message, ")");
        outgoingMessages.offer(message);
        if (currentMessage == null) {
            sendNext();
        }
    }

    protected void sendNext() {
        /* we clone the message so it doesn't change while we're
         * "transmitting".  We set the timestamp on the original message
         * before cloning, so the RepeatedPayload that is sending the message
         * sees that it was actually sent.  The timestamp is calculated as the
         * timestamp of when the message will be delivered.
         */
        SimTime txDelay;
        do {
            if (outgoingMessages.isEmpty()) {
                return;
            }
            currentMessage = outgoingMessages.poll();
            boolean dropMessage = false;
            for (NetworkFaultModel m : faultModels) {
                if (!m.canStart(currentMessage)) {
                    dropMessage = true;
                    dropLog("Fault model ",m," dropping ",currentMessage," at start time.");
                    //call all canStarts even if we get true so they will all get notification
                }
            }
            
            //compute the delay and set the timestamp here so that dropped messages
            //don't trigger a "failed to meet deadline" warning
            //if (bitWidth.isPositive()) {
                txDelay = SimTime.multiply(bitWidth, currentMessage.getSize());
            //} else {
            //    txDelay = SimTime.ZERO;
            //}
            currentMessage.setTimeStamp(SimTime.add(Harness.getTime(), txDelay));

            if (dropMessage) {
                currentMessage = null;
            }
        } while (currentMessage == null);

        
        currentMessage = currentMessage.clone();
        log("next message out is ", currentMessage,
                " (size=", currentMessage.getSize(), ", delay=", txDelay ,") @ ",
                currentMessage.getTimeStamp());
        currentMessage = currentMessage.clone();
        utilization.startUtilization();
        timer.start(txDelay);
    }

    public void timerExpired(Object callBackData) {
        log("enter timerExpired(", callBackData, ")");

        boolean isDropped = false;
        //check to see if we drop the message
        for (NetworkFaultModel fm : faultModels) {
            if (!fm.canDeliver(currentMessage)) {
                dropLog("Fault model ",fm," dropping ",currentMessage," at delivery time.");
                isDropped = true;
            //don't break here so that all the fault models get the canDeliver notification
            }
        }

        //deliver the message if it was not dropped
        if (!isDropped) {
            dropLog(currentMessage," not dropped.");
            log("delivering ", currentMessage);
            if (listeners.containsKey(currentMessage)) {
                for (Payload copyTo : listeners.get(currentMessage)) {
                    if (verbose) {
                        log("   to ", copyTo);
                    }
                    copyTo.copyFrom(currentMessage);
                }
            }
            if (receivers.containsKey(currentMessage)) {
                for (Pair<Networkable, ReadablePayload> p : receivers.get(currentMessage)) {
                    if (verbose) {
                        log("   to ", p.first);
                    }
                    p.second.deliverTo(p.first);
                }
            }
        }

        //start the next message if one exists
        currentMessage = null;
        utilization.endUtilization();
        if (!outgoingMessages.isEmpty()) {
            sendNext();
        }
        log("leave timerExpired()");
    }

    public NetworkUtilization getUtilization() {
        return utilization;
    }

    /* interface for registering fault models */
    public void registerFaultModel(NetworkFaultModel fm) {
        faultModels.add(fm);
        fm.registerNetworkScheduler(this);
    }

    public boolean unregisterFaultModel(NetworkFaultModel fm) {
        return faultModels.remove(fm);
    }

    public boolean isMessagePending() {
        if (currentMessage == null) {
            return false;
        }
        return true;
    }

    /*public Payload getPendingMessage() {
    return currentMessage;
    }*/
    public boolean dropCurrentMessage(NetworkFaultModel sourceFaultModel) {
        if (!isMessagePending()) {
            return false;
        }

        dropLog("Fault Model ",sourceFaultModel, " dropping pending message ", currentMessage);
        timer.cancel();
        currentMessage = null;
        utilization.endUtilization();
        if (!outgoingMessages.isEmpty()) {
            sendNext();
        }
        return true;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public void setDropVerbose(boolean dropVerbose) {
        this.dropVerbose = dropVerbose;
    }
    
    @Override
    public String toString() {
        return "NetSched";
    }

    protected void log(Object... o) {
        if (verbose) {
            Harness.log(toString(), o);
        }
    }
    
    protected void dropLog(Object... o) {
        if (verbose || dropVerbose) {
            Harness.log(toString(), o);
        }
    }
}
