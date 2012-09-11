/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework;


import jSimPack.SimTime;

/**
 * Prints out a log message periodically so that the simulator progress can be
 * observed if no logging messages are being printed.
 * @author Justin Ray
 */
public class ProgressLogger implements TimeSensitive {
    private SystemTimer t;
    private SimTime logInterval;
    private String name;
    //private long lastTime;
    
    public ProgressLogger(String name, SimTime logInterval) {
        this.name = name;
        this.logInterval = logInterval;
        t = new SystemTimer(this);
        t.start(logInterval);
        //lastTime = System.currentTimeMillis();
    }

    public void timerExpired(Object callbackData) {
        //long now = System.currentTimeMillis();
        //double realtimeRate = logInterval.getFracMilliseconds() / (double)(now - lastTime);
        //Harness.log(name, String.format("Progress indicator - True realtime rate=%.2f",realtimeRate));
        //lastTime = now;
        Harness.log(name, "Progress indicator");
        t.start(logInterval);
    }
}
