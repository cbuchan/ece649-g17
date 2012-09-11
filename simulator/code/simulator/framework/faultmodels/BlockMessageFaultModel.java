/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faultmodels;

import jSimPack.SimTime;
import simulator.framework.Logger;
import simulator.payloads.NetworkScheduler;
import simulator.payloads.Payload;
import simulator.payloads.WriteablePayload;

/**
 * Blocks a specific message for a specified period of time.  Messages can be blocked
 * at start time or delivery time.  Blocking at delivery means that the dropped 
 * messages consume bandwidth.
 * @author justinr2
 */
public class BlockMessageFaultModel extends Logger implements NetworkFaultModel {

    private NetworkScheduler ns = null;
    private final int dropType;
    private AbstractInterval faultInterval;
    private boolean dropAtDelivery = false;

    public BlockMessageFaultModel(WriteablePayload p, SimTime startTime, SimTime duration, boolean dropAtDelivery) {
        this(p.getType(), startTime, duration, dropAtDelivery);
    }

    public BlockMessageFaultModel(int dropType, SimTime startTime, SimTime duration, boolean dropAtDelivery) {
        super("BlockMessageFaultModel");
        this.dropType = dropType;
        this.dropAtDelivery = dropAtDelivery;
        this.faultInterval = new AbstractInterval(startTime, duration) {

            @Override
            public void startEvent() {
                log("Beginning blocking message ",Integer.toHexString(BlockMessageFaultModel.this.dropType) );
            }

            @Override
            public void endEvent() {
                if (ns == null) {
                    throw new NullPointerException("NetworkScheduler");
                }
                ns.unregisterFaultModel(BlockMessageFaultModel.this);
                log("End blocking message ",Integer.toHexString(BlockMessageFaultModel.this.dropType) );
            }
        };
    }

    public void registerNetworkScheduler(NetworkScheduler ns) {
        if (ns == null) {
            throw new NullPointerException("NetworkScheduler");
        }
        this.ns = ns;
    }

    public boolean canStart(Payload payload) {
        if (!dropAtDelivery && faultInterval.isActive() && payload.getType() == dropType) {
            //payload matches and drop is enabled, so don't allow starting
            log("Blocked message: ",Integer.toHexString(BlockMessageFaultModel.this.dropType) );
            return false;
        } else {
            //allow starting
            return true;
        }
    }

    public boolean canDeliver(Payload payload) {
        if (dropAtDelivery && faultInterval.isActive() && payload.getType() == dropType) {
            //payload matches and drop is enabled, so don't allow starting
            log("Blocked message: ",Integer.toHexString(BlockMessageFaultModel.this.dropType) );
            return false;
        } else {
            //allow starting
            return true;
        }
    }
}
