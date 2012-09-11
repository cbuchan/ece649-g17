/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework.faultmodels;

import jSimPack.SimTime;
import simulator.framework.Harness;
import simulator.framework.SystemTimer;
import simulator.framework.TimeSensitive;

/**
 * Time interval utility superclass.  Extend this object to respond to the start
 * and end events of a certain time interval.
 * @author justinr2
 */
public abstract class AbstractInterval implements TimeSensitive {
    private SystemTimer startTimer = new SystemTimer(this);
    private SystemTimer endTimer = new SystemTimer(this);
    private boolean active = false;
    
    private enum TimerObjects {
        START_OBJECT,
        END_OBJECT
    }
    
    public AbstractInterval (SimTime startTime, SimTime duration) {
        if (startTime.isBefore(Harness.getTime())) {
            throw new RuntimeException("Interval cannot start in the past.");
        } else if (startTime.equals(Harness.getTime())) {
            active = true;
        } else {
            //schedule the fault to start at some future time
            startTimer.start(startTime, TimerObjects.START_OBJECT);
            active = false;
        }
        if (!duration.isPositive()) {
            throw new RuntimeException("Interval duration must be positive");
        }
        if (!duration.equals(SimTime.FOREVER)) {
            //schedule an end time
            endTimer.start(SimTime.add(startTime, duration), TimerObjects.END_OBJECT);
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void timerExpired(Object callbackData) {
        switch((TimerObjects)callbackData) {
            case START_OBJECT:
                active = true;
                startEvent();
                break;
            case END_OBJECT:
                active = false;
                endEvent();
                break;
            default:
                throw new RuntimeException("Unrecognized callback object " + callbackData + " in SpecificMessageFaultModel");
        }
    }
    
    /**
     * Called when the interval starts.
     */
    public abstract void startEvent();

/**
     * Called when the interval ends.
     */
    public abstract void endEvent();

}
