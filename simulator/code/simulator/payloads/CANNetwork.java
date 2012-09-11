package simulator.payloads;

import jSimPack.SimTime;
import java.util.*;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;

/**
 * 
 * @author kamikasee
 */
public class CANNetwork extends NetworkScheduler {
   
    /**
     * This connector only allows time-triggered connections to the CAN network.
     */
    public class CanConnection {

        private NetworkConnection conn;

        public CanConnection() {
            conn = new Connection(null);
        }

        public void registerTimeTriggered(ReadableCanMailbox writeback) {
            conn.registerTimeTriggered(writeback);
        }
        
        public void sendTimeTriggered(WriteableCanMailbox msg, SimTime period) {
            registerCanSender(msg.asCanMailbox());
            conn.sendTimeTriggered(msg, period);
        }

        public void setEnabled(boolean enabled) {
            conn.setEnabled(enabled);
        }
    }

    public CANNetwork(SimTime bitWidth) {
        super(bitWidth);
    }


    private Set<Integer> canSenders = new HashSet<Integer>();

    /**
     * @return a time-triggered connection to the CAN network.  This is the
     * connection used by controllers
     */
    public CanConnection getCanConnection() {
        return new CanConnection();
    }

    /**
     * 
     * @return a connection that has time- and event- triggered interfaces
     */
    public Connection getFrameworkConnection()
    {
        return getFrameworkConnection(null);
    }

    /**
     * @param networkNode node to register with the connection for event triggered callbacks
     * @return a connection that has time- and event- triggered interfaces
     */
    public Connection getFrameworkConnection(Networkable networkNode)
    {
        return new Connection(networkNode);
    }


    /**
     * Informs the network that the specified message is the only one that
     * will have this message ID.  If another message with the same ID is
     * sent on the network, a runtime exception will be thrown.
     */
    private void registerCanSender(CanMailbox p) {
        if (canSenders.contains(p.getMessageId())) {
            throw new RuntimeException("a CAN message with ID " + p.getMessageId() + " is already being sent.");
        }
        canSenders.add(p.getMessageId());
    }

    @Override
        public String toString() {
            return "CANNetwork";
        }
}
