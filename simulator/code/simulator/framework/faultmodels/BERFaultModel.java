/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faultmodels;

import jSimPack.SimTime;
import java.util.Random;
import simulator.framework.Harness;
import simulator.framework.Logger;
import simulator.payloads.NetworkScheduler;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;
import simulator.payloads.Payload;

/**
 * Kills a message at exponentially distributed intervals with a mean of 
 * 1/inverseBitErrorRate.  If no message is being sent when the bit error occurs,
 * then the error has no effect.
 * @author justinr2
 */
public class BERFaultModel extends Logger implements NetworkFaultModel, TimeSensitive {

    private final long inverseBitErrorRate;
    private NetworkScheduler ns;
    private final Random random;
    private Timer timer = new Timer(this);
    private long dropCount = 0;
    private SimTime startTime;
    /**
     * 
     * @param inverseBitErrorRate - Inverse of the bit error rate.  If the bit error rate
     * is lambda (1E-3/s), then specify * 1000 (1e3)
     * @param verbose
     */
    public BERFaultModel(long inverseBitErrorRate) {
        super("BERFaultModel");
        this.inverseBitErrorRate = inverseBitErrorRate;
        this.random = Harness.getRandomSource().getRandom();
        log("Inverse Bit Error Rate=",inverseBitErrorRate);
        this.startTime = Harness.getTime();
    }

    public void registerNetworkScheduler(NetworkScheduler ns) {
        if (ns == null) throw new NullPointerException("NetworkScheduler ns");
        this.ns = ns;
        if (ns.getBitWidth().equals(SimTime.ZERO)) {
            System.err.println("Warning:  BER fault model will be disabled because network bit width is zero.");
            log("Warning:  BER fault model will be disabled because network bit width is zero.");
        } else {
            scheduleNextError();
        }
    }

    private void scheduleNextError() {
        long nextTime = (long)(Math.log(random.nextDouble())*(double)inverseBitErrorRate*-1);
        log("Next bit error in ", nextTime, " bit times");
        timer.start(SimTime.multiply(ns.getBitWidth(), nextTime));
    }

    public boolean canStart(Payload payload) {
        //always return true
        return true;        
    }

    public boolean canDeliver(Payload payload) {
        //always return true
        return true;
    }

    public void timerExpired(Object callbackData) {
        if (ns.isMessagePending()) {
            log("Dropping current message.");
            ns.dropCurrentMessage(this);
            dropCount++;
        } else {
            log("Bit errror occured when network not active so bit error had no effect.");
        }
        scheduleNextError();
    }
    
    public String getStats() {
        SimTime elapsed = SimTime.subtract(Harness.getTime(), startTime);
        return String.format("Dropped %d messages in %s seconds - avg BER rate=%.2f", 
                dropCount, 
                elapsed.toString(SimTime.SimTimeUnit.SECOND), 
                (double)dropCount / elapsed.getFracSeconds());
    }
}
