package simulator.payloads;

import jSimPack.SimTime;
import simulator.framework.Direction;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;
import simulator.payloads.HallCallPayload.ReadableHallCallPayload;
import simulator.payloads.HallCallPayload.WriteableHallCallPayload;
import simulator.payloads.PhysicalPayload.PhysicalReadablePayload;
import simulator.payloads.PhysicalPayload.PhysicalWriteablePayload;


/**
 * Implement physical state representation of the system as a message passing
 * network.  Physical state "messages" are non-blocking, which means they are
 * received in the same simulation instant that they are sent.
 *
 * @author justinr2
 */
public class PhysicalNetwork extends NetworkScheduler
{

    /**
     * Connects elevator components to the physical framework. This class allows
     * elevator controllers to send and receive messages to and from framework
     * components, such as the buttons, doors, and drive motor. The component must
     * send messages of the same type it receives, although it may choose to only
     * perform only of those two functions. If the component tries to send or
     * receive two messages of different types, or it tries to send a message of a
     * different type than it registers to receive, a runtime exception will occur.
     *
     * This connector only provides time-triggered acccess to the physical framework.
     *
     */
    public class PhysicalConnection //implements NetworkConnection
    {

        private NetworkConnection conn;
        private boolean isRegistered;
        private boolean isSending;

        public PhysicalConnection(Networkable connectedTo)
        {
            conn = new Connection(connectedTo);
            isRegistered = false;
            isSending = false;
        }
        
        public PhysicalConnection()
        {
            this(null);
        }

        public void registerTimeTriggered(PhysicalReadablePayload writeback)
        {
            if (isRegistered)
                throw new RuntimeException(
                "Already registered to recieve a physical framework message.  Only one is allowed.");

            if (writeback.asPayload() instanceof InternalPayload) {
                throw new RuntimeException("Receiving a payload marked as InternalPayload is not permitted through this interface.  See the note in simulator.payloads.InternalPayload for details.");
            }

            isRegistered = true;
            conn.registerTimeTriggered(writeback);
        }

        
        /*public void registerEventTriggered(Payload writeback)
        {
            if (isRegistered)
                throw new RuntimeException(
                "already registered for a physical framework message.");

            isRegistered = true;
            conn.registerEventTriggered(writeback);
        }*/

        public void sendTimeTriggered(PhysicalWriteablePayload msg, SimTime period)
        {
            if (isSending)
                throw new RuntimeException(
                "already sending a physical framework message.  Only one is allowed.");

            if (msg.asPayload() instanceof InternalPayload) {
                throw new RuntimeException("Sending a payload marked as InternalPayload is not permitted through this interface.  See the note in simulator.payloads.InternalPayload for details.");
            }

            isSending = true;
            conn.sendTimeTriggered(msg, period);
        }

        /*@Override
        public void sendOnce(Payload msg)
        {
            if (isSending && msg.getType() != typeSent)
                throw new IllegalArgumentException(
                "cannot send more than one type of message");

            isSending = true;
            typeSent = msg.getType();
            conn.sendOnce(msg);
        }
        public void checkPayload(Payload payload) {
            if (!(payload instanceof PhysicalPayload)) {
                throw new RuntimeException("Payloads to the Physical network must be PhysicalPayloads");
            }
        }*/
        
        public void setEnabled(boolean enabled)
        {
            conn.setEnabled(enabled);
        }
    }
    
    public PhysicalNetwork()
    {
        super();
    }

    /**
     * Get a connection that is limited to time-triggered interfaces
     * @param networkNode
     * @return
     */
    public PhysicalConnection getConnection(Networkable networkNode)
    {
        return new PhysicalConnection(networkNode);
    }

    /**
     * Get a connection that is limited to time-triggered interfaces
     * @return
     */
    public PhysicalConnection getConnection()
    {
        return new PhysicalConnection(null);
    }

    /**
     * Get a connection with both time- and event-triggered interfaces.
     * This is used for system objects and runtime monitors, and may not be used
     * by students directly.
     *
     * @return
     */
    public Connection getFrameworkConnection()
    {
        return getFrameworkConnection(null);
    }

     /**
     * Get a connection with both time- and event-triggered interfaces.
     * This is used for system objects and runtime monitors, and may not be used
     * by students directly.
     * 
     * @return
     */
    public Connection getFrameworkConnection(Networkable networkNode)
    {
        return new Connection(networkNode);
    }
    
    @Override
    public String toString()
    {
        return "PhysNetwork";
    }
    
    public static void main(String args[]) {
        PhysicalNetwork network = Harness.getPhysicalNetwork();
        
        EventPrinter ep = new EventPrinter(network);
        EventSender es = new EventSender(network);
        
        Harness.setRealtimeRate(1.0);
        Harness.runSim(new SimTime(5, SimTime.SimTimeUnit.SECOND));
    }

    public static class EventSender extends Networkable implements TimeSensitive {
        public NetworkConnection conn;
        public Timer t = new Timer(this);
        public WriteableHallCallPayload h = HallCallPayload.getWriteablePayload(7, Hallway.FRONT, Direction.DOWN);
        public SimTime period = new SimTime(100, SimTime.SimTimeUnit.MILLISECOND);
        
        public EventSender(PhysicalNetwork pn) {
            conn = pn.getFrameworkConnection(this);
            conn.sendTimeTriggered(h, period);
            t.start(period);
        }

        public void timerExpired(Object callbackData) {
            h.set(!h.pressed());
            System.out.println(Harness.getTime() + ":  Setting value to: " + h);
            //conn.sendOnce(h);
            t.start(period);
        }
        
    }

    
    public static class EventPrinter extends Networkable implements TimeSensitive {
        public PhysicalConnection conn;
        public Timer t = new Timer(this);
        public ReadableHallCallPayload h = HallCallPayload.getReadablePayload(7, Hallway.FRONT, Direction.DOWN);
        public SimTime period = new SimTime(50, SimTime.SimTimeUnit.MILLISECOND);
        
        public EventPrinter(PhysicalNetwork pn) {
            conn = pn.getConnection(this);
            conn.registerTimeTriggered(h);
            t.start(period);
        }

        public void timerExpired(Object callbackData) {
            if (h.pressed()) {
                System.out.println(Harness.getTime() + ":  TT read: " + h + "*************");
            } else {
                System.out.println(Harness.getTime() + ":  TT read: " + h);
            }
            t.start(period);
        }
    }
    
}