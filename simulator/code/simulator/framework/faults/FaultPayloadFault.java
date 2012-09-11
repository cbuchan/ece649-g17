/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import java.text.ParseException;
import java.util.ArrayList;
import simulator.framework.Harness;
import simulator.payloads.NetworkConnection;
import simulator.framework.ReflectionFactory;
import simulator.framework.faultmodels.AbstractInterval;
import simulator.payloads.FaultPayload.WriteableFaultPayload;

/**
 * Creates and injects a fault payload on the physical network
 * @author justinr2
 */
public class FaultPayloadFault extends Fault {

    private final AbstractInterval faultInterval;
    private WriteableFaultPayload payload = null;
    private NetworkConnection pconn;
    
    /**
     * Arguments:
     * -start time
     * -duration
     * -name of the fault payload.  "FaultPayload" is automatically concatenated
     * to reduce the chance of instantiating the wrong kind of object.
     * -constructor arguments to be passed to fault payload constructor by reflection
     * 
     * @param args
     * @throws ParseException
     */
    public FaultPayloadFault(String args[]) throws ParseException {
        super("FaultPayload: " + args[2]);
        if (args.length < 3) {
            throw new ParseException("FaultPayloadFault requires at least 3 arguments", 0);
        }
        //construct the fault payload
        ReflectionFactory refFactory = new ReflectionFactory(false);
        ArrayList<String> methodArgs = new ArrayList<String>();
        for (int i=3; i < args.length; i++) {
            methodArgs.add(args[i]);
        }

        try {
            payload = (WriteableFaultPayload)refFactory.createFromFactoryMethod(args[2], null, "createWriteableFaultPayload", methodArgs);
        } catch (Exception ex) {
            throw new RuntimeException("FaultPayloadFault: " + ex);
        }

        //get a connection for sending
        pconn = Harness.getPhysicalNetwork().getFrameworkConnection();

        //create an interval to send the payload
        try {
            faultInterval = new AbstractInterval(FaultUtility.parseTime(args[0]), 
                    FaultUtility.parseTime(args[1])) {

                @Override
                public void startEvent() {
                    payload.setEnabled(false);
                    log("Injecting fault payload: ",payload);
                    pconn.sendOnce(payload);
                }

                @Override
                public void endEvent() {
                    payload.setEnabled(true);
                    log("Injecting fault payload: ",payload);
                    pconn.sendOnce(payload);
                }                
            };
        } catch (NumberFormatException ex) {
            throw new ParseException("FaultPayloadFault:  " + ex.getMessage(), 0);
        }
    }
}
