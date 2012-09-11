/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import simulator.framework.faultmodels.*;
import java.text.ParseException;
import simulator.framework.Harness;
import simulator.framework.Logger;

/**
 * Registers network faults like bit error rate and drop messages.
 *
 * Only one network fault can be created in any simulation.
 *
 * @author justinr2
 */
public class NetworkFault extends Fault {

    protected static NetworkFault networkFaultRegistered = null;
    protected final Logger faultModelLogger; //keep a reference to the fault model for logging purposes.
    protected final BERFaultModel bfm;
    protected final DropMessagesFaultModel dmfm;
    
    public enum NetworkFaultType {
        BitErrorRate,
        DropMessages
    }
    private final NetworkFaultType type;

    /**
     * Arguments:
     * -fault type (one of NetworkFaultType)
     * -remaining arguments parsed by specific fault types
     * 
     * @param args
     * @throws ParseException
     */
    public NetworkFault(String[] args) throws ParseException {
        if (networkFaultRegistered != null) {
            throw new ParseException("NetworkFault:  You cannot create multiple network faults.  Fault of type " + networkFaultRegistered.getType() + " already registered.", 0);
        }
        if (args.length < 2) {
            throw new ParseException("NetworkFault:  Must have at least two arguments.", 0);
        }
        int index = 0;
        try {
            type = NetworkFaultType.valueOf(args[index]);
        } catch (IllegalArgumentException ex) {
            throw new ParseException("NetworkFault:  no network fault of type " + args[index], 0);
        }
        index++;
        NetworkFaultModel fm = null;
        switch (type) {
            case BitErrorRate:
                
                bfm = new BERFaultModel(FaultUtility.parseLong(args[1]));
                dmfm = null;
                fm = bfm;
                faultModelLogger = bfm;
                setName("NetworkFault-BER");
                break;
            case DropMessages:
                dmfm = new DropMessagesFaultModel(FaultUtility.parseDouble(args[1], 0, 100));
                bfm = null;
                fm = dmfm;
                faultModelLogger = dmfm;
                setName("NetworkFault-DroppedMessages");
                break;
            default:
                throw new ParseException("NetworkFault:  unrecognised type " + type, 0);
        }
        log("Creating network fault");
        //register fault
        Harness.getCANNetwork().registerFaultModel(fm);
    }

    @Override
    public void setVerbose(boolean verbose) {
        super.setVerbose(verbose);
        faultModelLogger.setVerbose(verbose);
    }
    
    public NetworkFaultType getType() {
        return type;
    }

    @Override
    public String getFaultStats() {
        if (dmfm != null) {
            return name + ": " + dmfm.getStats();
        }
        if (bfm != null) {
            return name + ": " +  bfm.getStats();
        }
        return "";
    }
}
