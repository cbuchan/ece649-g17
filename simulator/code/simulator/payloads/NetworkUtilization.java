/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.payloads;

import jSimPack.SimTime;
import simulator.framework.Harness;
import simulator.framework.SystemTimer;
import simulator.framework.TimeSensitive;

/**
 * Utility class for keeping track of the the amount of time the network is used
 * vs. idle.
 *
 * @author justinr2
 */
public class NetworkUtilization implements TimeSensitive {

    private boolean verbose = false;
    private boolean enabled = true;
    private long totalUtilizedMicroseconds = 0;
    private long recentUtilizedMicroseconds = 0;
    private SimTime lastStart = null;
    private SimTime recentUtilizationPeriod = new SimTime(1, SimTime.SimTimeUnit.SECOND);
    private SystemTimer timer = new SystemTimer(this);
    private double overallUtilization;
    private double recentUtilization;
    private double maxUtilization;
    private String name;

    NetworkUtilization(String name) {
        this(name, true);
    }

    NetworkUtilization(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
        if (enabled) {
            timer.start(recentUtilizationPeriod);
            overallUtilization = 0;
            recentUtilization = 0;
            maxUtilization = 0;
        } else {
            overallUtilization = -1;
            recentUtilization = -1;
            maxUtilization = -1;
        }
    }

    void startUtilization() {
        if (enabled) {
            lastStart = Harness.getTime();
        }
    }

    void endUtilization() {
        if (enabled) {
            if (lastStart == null) {
                throw new RuntimeException("Called endUtilization without startUtilization");
            }
            SimTime end = Harness.getTime();
            long timeUsed = end.getTruncMicroseconds() - lastStart.getTruncMicroseconds();
            totalUtilizedMicroseconds += timeUsed;
            recentUtilizedMicroseconds += timeUsed;
            lastStart = null;
        }
    }

    public void timerExpired(Object callbackData) {
        //don't check enabled because a disabled utilization object won't have the timer started
        SimTime now = Harness.getTime();
        if (lastStart != null) {
            //compute the partial utilization from the start to the present time
            long utilization = now.getTruncMicroseconds() - lastStart.getTruncMicroseconds();
            totalUtilizedMicroseconds += utilization;
            recentUtilizedMicroseconds += utilization;
            //change the start time to reflect the part we have already logged
            lastStart = now;
        }

        overallUtilization = (double) totalUtilizedMicroseconds / (double) now.getTruncMicroseconds();
        recentUtilization = (double) recentUtilizedMicroseconds / (double) getRecentUtilizationPeriod().getTruncMicroseconds();
        if (getRecentUtilization() > getMaxUtilization()) {
            maxUtilization = getRecentUtilization();
        }

        if (verbose) {
            Harness.log(name, toString());
        }
        
        //set up for next run
        recentUtilizedMicroseconds = 0;
        timer.start(recentUtilizationPeriod);
    }

    public double getRecentUtilization() {
        return recentUtilization;
    }

    public SimTime getRecentUtilizationPeriod() {
        return recentUtilizationPeriod;
    }

    public double getOverallUtilization() {
        return overallUtilization;
    }

    public double getMaxUtilization() {
        return maxUtilization;
    }

    @Override
    public String toString() {
        if (enabled) {
            return String.format("Recent: %.2f %% Max: %.2f %%  Overall:  %.2f %%", 
                    getRecentUtilization()*100,
                    getMaxUtilization()*100, 
                    getOverallUtilization()*100);
        } else {
            return "Utilization disabled";
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
