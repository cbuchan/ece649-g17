/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework;

import jSimPack.FutureEvent;
import jSimPack.FutureEventListener;
import jSimPack.SimTime;

/**
 * Allows elevator modules to schedule themselves to execute at specified
 * times in the future.  Such modules must implement {@link TimeSensitive}.
 * When a <code>Timer</code> is created, it is associated with
 * a <code>TimeSensitive</code> object called the "handler".  When the handler
 * calls {@link #start}, it specifies a time interval after which this
 * <code>Timer</code> "expires".  When this <code>Timer</code> expires, it
 * will call the handler's {@link TimeSensitive#timerExpired(Object)} method.
 * This <code>Timer</code> does not automatically repeat; the handler must
 * call <code>start</code> after each time <code>timerExpired</code> is
 * executed.  If a handler creates more than one <code>Timer</code>, it can
 * distinguish between their calls to <code>timerExpired</code> by specifying
 * different callback objects to the {@link #start(long, Object)} method of
 * each <code>Timer</code>.
 *
 * This abstract class can be implemented on different time bases by subclassing
 * and specifying the scheduleEvents() and cancelEvent() methods
 * 
 * @author William Nace
 * @author Kenny Stauffer
 */
public abstract class AbstractTimer implements FutureEventListener {

    private final TimeSensitive handler;
    //private HashMap<Object, FutureEvent> pendingEvents = new HashMap<Object, FutureEvent>();
    private FutureEvent pendingEvent = null;
    // Verbosity level: 0 = no messages, not even errors
    //                  1 = errors
    //                  2 = errors + dot on scheduled, - on cancel + on ring
    //                  3 = errors + message on each schedule
    //                  4 = errors + message on each schedule/cancel/ring
    //                 10 = above + entire Future Event List on sched/can/ring
    private static int verbose = 0;

    //event scheduling abstract methods
    /**
     * Schedule the timer event - this method is implemented to schedule the Timer
     * on a specific event queue.
     * @param event Event to be scheduled
     * @param timeInterval Amount of time in the future to schedule the event
     * @param data Callback data for the timerExpired callback
     * @return the event object that was created.  This can be used to cancel a timer event.
     */
    protected abstract FutureEvent scheduleEvent(FutureEventListener event, SimTime timeInterval, Object data);

    /**
     * Cancel a pending event - this method is implemented to cancel Timer events
     * on a specific event queue.
     * @param event event to cancel
     * @return true if the event was cancelled
     */
    protected abstract void cancelEvent(FutureEvent event);

    /**
     * Sets the logging verbosity for all <code>Timer</code> objects.
     * @param verbose
     */
    protected static void setVerbosity(int verbose) {
        AbstractTimer.verbose = verbose;
    }

    /**
     * Constructs a Timer with the specified handler.  When the
     * <code>Timer</code> expires, it will call back to the handler's
     * <code>timerExpired(Object)</code> method.
     */
    public AbstractTimer(TimeSensitive handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        this.handler = handler;
    }

    /**
     * Schedules this <code>Timer</code> to expire after the specified time
     * interval has passed.  When it expires, it will pass <code>null</code>
     * to <code>timerExpired</code>.
     *
     * @throws IllegalArgumentException
     * if <code>nanoSeconds</code> is negative 
     */
    public void start(SimTime startTime) {
        start(startTime, null);
    }

    /**
     * Schedules this <code>Timer</code> to expire after the specified time
     * interval has passed.  When it expires, it will pass
     * <code>callback</code> to <code>timerExpired</code>.
     *
     * The timer does not automatically repeat.  You must call start() every
     * time you want to use the timer.
     *
     * @throws IllegalArgumentException
     * if <code>nanoSeconds</code> is negative
     */
    public void start(SimTime timerDuration, Object callback) {
        //JR get rid of this check because the duration is already validated inside the
        //event list
        //minor optimization, but this call is on the critical path for the simulator.
        /*if (!timerDuration.isNonNegative()) {
        throw new IllegalArgumentException("nanoSeconds is negative");
        }*/

        log("start(", timerDuration, ",", callback, ")");

        cancel();

        pendingEvent = scheduleEvent(this, timerDuration, callback);
    }

    /**
     * Returns <code>true</code> if and only if the <code>Timer</code> is
     * scheduled to expire.
     */
    public boolean isRunning() {
        return pendingEvent != null;
    }

    /**
     * Stops the <code>Timer</code> so it will not call back to the handler.
     * If the <code>Timer</code> is not running, this method does nothing.
     *
     */
    public void cancel() {
        if (pendingEvent == null) {
            return;
        }

        cancelEvent(pendingEvent);
        pendingEvent = null;
    }

    private void log(Object... o) {
        if (verbose > 10) {
            Harness.log(toString(), o);
        }
    }

    @Override
    public String toString() {
        return "Timer(handler=" + handler + ",pendingEvent=" + pendingEvent + ")";
    }

    public void eventReleased(Object data) {
        log("TimerEventReleased", data);
        pendingEvent = null;
        handler.timerExpired(data);
    }
}
