/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faultmodels;

import jSimPack.SimTime;
import simulator.framework.Harness;
import simulator.framework.Logger;
import simulator.payloads.NetworkScheduler;
import simulator.framework.SystemTimer;
import simulator.framework.TimeSensitive;
import simulator.payloads.Payload;

/**
 * Fault model that blocks all network traffic for a specified duration.
 * @author kamikasee
 */
public class BlackoutFaultModel extends Logger implements NetworkFaultModel, TimeSensitive {

    private NetworkScheduler ns = null;
    private boolean isBlackedOut = false;
    private SystemTimer timer = new SystemTimer(this);
    private SimTime lastBlackoutStart;
    private SimTime totalBlackoutDuration = SimTime.ZERO;
    private long dropCount = 0;

    public BlackoutFaultModel(boolean verbose) {
        super("BlackoutFaultModel", verbose);
        this.verbose = true;
    }
    
    public void registerNetworkScheduler(NetworkScheduler ns) {
        if (ns == null) {
            throw new NullPointerException("NetworkScheduler ns");
        }
        this.ns = ns;
    }

    public void blackoutStart(SimTime duration) {
        if (ns != null) {
            isBlackedOut = true;
            if (timer.isRunning()) {
                //System.err.println(Harness.getTime() + "  Blackout restarted");   
                timer.start(duration);
                log("Blackout restart");
            } else {
                //System.err.println(Harness.getTime() + "  Blackout started");   
                timer.start(duration);
                log("Blackout started");
                lastBlackoutStart = Harness.getTime();
            }
        } else {
            throw new RuntimeException("No network scheduler registered.");
        }
    }

    public boolean canStart(Payload payload) {
        //always return true
        return true;
    }

    public boolean canDeliver(Payload payload) {
        if (isBlackedOut) {
            log("Blackout in progress");
            dropCount++;
            return false;
        } else {
            return true;
        }
    }

    public void timerExpired(Object callbackData) {
        //the blackout has ended
        //drop the pending message
        ns.dropCurrentMessage(this);
        dropCount++;
        isBlackedOut = false;
        log("Blackout ended");
        SimTime elapsed = SimTime.subtract(Harness.getTime(), lastBlackoutStart);
        totalBlackoutDuration = SimTime.add(totalBlackoutDuration, elapsed);
    }

    @Override
    public String toString() {
        return "BlackoutFaultModel";
    }
    
    public String getStats() {
        return String.format("Total blackout duration: %s; Drop count=%d",
                totalBlackoutDuration.toString(SimTime.SimTimeUnit.SECOND),
                dropCount);
    }
    
}
