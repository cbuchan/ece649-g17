/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework.faultmodels;

import simulator.payloads.NetworkScheduler;
import simulator.framework.*;
import simulator.payloads.Payload;

/**
 * Interface for defining fault model objects.
 * 
 * The fault model can decide how to drop faults based on other information, but
 * this interface is used to hook the fault model into the network scheduler.
 * 
 * @author justinr2
 */
public interface NetworkFaultModel {
    /**
     * Give the fault model a reference to the network scheduler.  This allows
     * it to call the fault interface methods, such as isMessagePending and
     * dropCurrentMessage.
     * @param ns The network scheduler this fault model is registered with.
     */
    public void registerNetworkScheduler(NetworkScheduler ns);
    /**
     * Notify the fault model that a message is starting
     * @param payload Message that is starting
     */
    public boolean canStart(Payload payload);
    
    /**
     * Ask fault model if the message can be delivered
     * @param payload Message to be delivered
     * @return true if the message can be delivered
     */
    public boolean canDeliver(Payload payload);

}
