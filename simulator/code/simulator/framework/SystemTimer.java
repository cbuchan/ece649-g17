package simulator.framework;

import jSimPack.FutureEvent;
import jSimPack.FutureEventListener;
import jSimPack.SimTime;



/**
 * This class implements timer functionality for the system queue.  Events
 * scheduled on the system queue are not randomized and always occur before 
 * events in the simulation queue scheduled at the same time.  
 * 
 * This timer may not be used in any code written by a student, except when
 * implementing RuntimeMonitors.
 * @author justinr2
 */
public class SystemTimer extends AbstractTimer {

    public SystemTimer(TimeSensitive handler) {
        super(handler);
    }
    
    @Override
    protected FutureEvent scheduleEvent(FutureEventListener event, SimTime timeInterval, Object data) {
        return Harness.scheduleNonsimulationEvent(event, timeInterval, data);
    }

    @Override
    protected void cancelEvent(FutureEvent event) {
        Harness.cancelNonsimulationEvent(event);
    }
    
}
