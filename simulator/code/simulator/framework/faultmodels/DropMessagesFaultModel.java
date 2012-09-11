/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faultmodels;

import simulator.payloads.NetworkScheduler;
import java.util.HashMap;
import simulator.framework.*;
import java.util.Random;
import simulator.payloads.Payload;

/**
 * This fault model drops the specified percentage of messages, but never two
 * consecutive messages with the same type.
 * 
 * @author justinr2
 */
public class DropMessagesFaultModel extends Logger implements NetworkFaultModel {

    private NetworkScheduler ns;
    private double dropPercentage;
    private Random random;
    //private Payload lastDroppedPayload = null;
    //private boolean deferredDrop = false;
    /**
     * Stores drop history - if a value is true, then the last message of that type
     * was dropped.
     */
    private HashMap<Integer, Boolean> dropHistory = new HashMap<Integer, Boolean>();
    private long dropCount = 0;
    private long notDropCount = 0;

    /**
     * 
     * @param dropPercentage A value in the range [0,100] that indicates how many messages should be dropped.
     */
    public DropMessagesFaultModel(double dropPercentage) {
        super("DropMessageFaultModel");
        if (dropPercentage < 0 || dropPercentage > 100) {
            throw new RuntimeException("Drop percentage out of range [0,100]: " + dropPercentage);
        }
        this.dropPercentage = dropPercentage / 100; //store as a value in the range [0.1]
        this.random = Harness.getRandomSource().getRandom();
    }

    public void registerNetworkScheduler(NetworkScheduler ns) {
        if (ns == null) {
            throw new NullPointerException("NetworkScheduler");
        }
        this.ns = ns;
    }

    public boolean canDeliver(Payload payload) {
        boolean dropMessage = false;
        if (!dropHistory.containsKey(payload.getType())) {
            //don't drop the first message
            dropMessage = false;
        } else if (dropHistory.get(payload.getType()) == true) {
            //don't drop the message because it was previously dropped
            dropMessage = false;
        } else if (random.nextDouble() <= dropPercentage) {
            //okay to drop the message, do so randomly
            dropMessage = true;
            log("Message ", payload, " marked for dropping.");
        }

        //remember whether or not we dropped the message
        dropHistory.put(payload.getType(), dropMessage);
        
        //keep track of drop stats
        if (dropMessage) {            
            dropCount++;
        } else {
            notDropCount++;
        }
        return !dropMessage;
    }

    public boolean canStart(Payload payload) {
        //always return true
        return true;
    }

    public String getStats() {
        return String.format("Drop count=%d, Not Dropped=%d, Percentage=%.2f",
                dropCount,
                notDropCount,
                (double) dropCount / (double) (dropCount + notDropCount));
    }
}
