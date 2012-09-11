/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.payloads;

/**
 * No Fault payloads are currently implemented, but if you wanted to fault
 * specific objects, you can create a new payload class that descends from this
 * class and use simulator.faults.FaultPayloadFault to inject it at certain times
 * using the -ff file.
 *
 * @author Justin Ray
 */
public abstract class FaultPayload extends PhysicalPayload {

    public abstract class ReadableFaultPayload extends PhysicalReadablePayload {
        private FaultPayload payload;
        protected ReadableFaultPayload(FaultPayload payload) {
            super(payload);
            this.payload = payload;
        }
        public boolean isEnabled() {
            return payload.isEnabled();
        }

        FaultPayload asFaultPayload() {
            return payload;
        }
    }

    public static class WriteableFaultPayload extends PhysicalWriteablePayload {
        private FaultPayload payload;
        protected WriteableFaultPayload(FaultPayload payload) {
            super(payload);
            this.payload = payload;
        }
        public boolean isEnabled() {
            return payload.isEnabled();
        }
        public void setEnabled(boolean enabled) {
            payload.setEnabled(enabled);
        }

        FaultPayload asFaultPayload() {
            return payload;
        }

    }

    protected boolean enabled = true;
    
    public FaultPayload(FaultPayload p) {
        super(p);
        this.enabled = p.enabled;
    }
    
    public FaultPayload(int type, int replicationID) {
        super(type, replicationID);
    }
    
    public FaultPayload(int type) {
        super(type);
    }

    @Override
    public void copyFrom(Payload p) {
        super.copyFrom(p);
        this.enabled = ((FaultPayload)p).enabled;
    }
    
    
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public boolean isEnabled() {
        return enabled;
    }
}
