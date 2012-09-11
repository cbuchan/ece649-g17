package simulator.framework;

import jSimPack.FutureEvent;
import jSimPack.FutureEventListener;
import jSimPack.SimTime;


/**
 * Timer for simulation objects.  Timer callback events are mixed in with other
 * simulation events.
 *
 * @author Justin Ray
 */

public class Timer extends AbstractTimer {

    public Timer(TimeSensitive handler) {
        super(handler);
    }
    
    @Override
    protected FutureEvent scheduleEvent(FutureEventListener event, SimTime timeInterval, Object data) {
        return Harness.schedule(event, timeInterval, data);
    }

    @Override
    protected void cancelEvent(FutureEvent event) {
        Harness.cancelEvent(event);
    }

    public static void main(String[] args) {
        Timer t = null;
        Test test = new Test();
        t = new Timer(test);
        test.setTimer(t);
        t.setVerbosity(15);

        t.start(new SimTime(1, SimTime.SimTimeUnit.SECOND), true);
        t.start(new SimTime(2, SimTime.SimTimeUnit.SECOND), 1);
        t.start(new SimTime(3, SimTime.SimTimeUnit.SECOND), new String("argyle"));
        t.start(new SimTime(2.5, SimTime.SimTimeUnit.SECOND));

        Harness.runSim(new SimTime(50, SimTime.SimTimeUnit.SECOND));

    }

    private static class Test implements TimeSensitive {

        Timer t;
        
        public void setTimer(Timer t) {
            this.t = t;
        }
        
        public void timerExpired(Object callbackData) {
            System.out.println("@" + Harness.getTime() + " timerExpired:  " + callbackData);
            if (callbackData == null) {
                t.cancel();
            }
        }
    }
    
    
}
