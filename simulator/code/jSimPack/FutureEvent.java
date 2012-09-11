package jSimPack;

/**
 * Data object that encodes event information.  These objects are scheduled and
 * executed by FutureEventList.
 * 
 */
public class FutureEvent implements Comparable<FutureEvent>
{
    final FutureEventListener handler;
    final SimTime when;
    final Object callback;
    private boolean isScheduled;

    /**
     * 
     * @param handler the class that gets the callback when the event occurs
     * @param when wall clock time for the event to happen
     * @param callback data that is passed back to the listener when the event occurs
     * this allows listeners that get more than one event to distinguish between
     * them.
     */
    FutureEvent(FutureEventListener handler, SimTime when, Object callback)
    {
	this.handler = handler;
	this.when = when;
	this.callback = callback;
        isScheduled = true;
    }

    /**
     * 
     * @return true if the event has been scheduled but has not occured yet
     */
    public boolean isScheduled()
    {
        return isScheduled;
    }

    /**
     * mark the event as having occurred
     */
    public void expire()
    {
        if (!isScheduled)
            throw new IllegalStateException("event is not scheduled: "+this);
        isScheduled = false;
    }

    /**
     * Implementation of the Comparable interface - comparison is based on 
     * event time.
     */
    public int compareTo(FutureEvent e)
    {
        return when.compareTo(e.when);
    }

    @Override
    public String toString() {
        return "Time:" + when + " Callback" + callback;
    }
}
