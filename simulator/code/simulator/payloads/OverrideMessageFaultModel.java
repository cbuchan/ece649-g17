/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.payloads;

import jSimPack.SimTime;
import simulator.framework.Logger;
import simulator.framework.faultmodels.AbstractInterval;
import simulator.framework.faultmodels.NetworkFaultModel;

/**
 * Overrides a specific message type with the payload value specified at creation
 * time.  
 * @author justinr2
 */
public class OverrideMessageFaultModel extends Logger implements NetworkFaultModel {

    private NetworkScheduler ns = null;
    private final WriteablePayload dropPayload;
    private AbstractInterval faultInterval;

    public OverrideMessageFaultModel(WriteablePayload dropPayload, SimTime startTime, SimTime duration) {
        super("OverrideMessageFault");
        this.dropPayload = dropPayload;
        this.faultInterval = new AbstractInterval(startTime, duration) {

            @Override
            public void startEvent() {
            //do nothing
                log("Beginning override with message ",OverrideMessageFaultModel.this.dropPayload );
            }

            @Override
            public void endEvent() {
                if (ns == null) {
                    throw new NullPointerException("NetworkScheduler");
                }
                ns.unregisterFaultModel(OverrideMessageFaultModel.this);
                log("Ending override with message ",OverrideMessageFaultModel.this.dropPayload );
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
        //always allow transmission
        return true;
    }

    public boolean canDeliver(Payload payload) {
        if (faultInterval.isActive() && payload.getType() == dropPayload.getType()) {
            //payload matches and drop is enabled, so don't allow starting
            //copy the timestamp from the payload before overriding
            dropPayload.setTimeStamp(payload.getTimeStamp());
            payload.copyFrom(dropPayload.asPayload());
            log("Overriding with ",dropPayload);
        } else {
        //don't modify other messages
        }
        return true;
    }
}
