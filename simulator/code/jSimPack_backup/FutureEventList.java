package jSimPack;

import jSimPack.SimTime.SimTimeUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dispatches abstract events that occur at discrete points in time.  Events
 * are dispatched in increasing order of their release time.  The release time
 * of an event is calculated when it is scheduled via  {@link
 * #schedule(FutureEventListener,long,Object)}.  Each event is associated with
 * an <code>FutureEventListener</code> that takes an arbitrary action when the
 * event occurs.  While an <code>FutureEventListener</code> is processing an
 * event, it can freely add further events to, or remove other events from,
 * the event queue.  A <code>FutureEventList</code> keeps track of the current
 * time (as a <code>long</code>) in the simulation, so an event can never be
 * added with a timestamp in the "past" (before the current time), but events
 * can be removed before they are processed.  
 * 
 * The FutureEventList has a setting called realtimeRate.  This allows the simulation
 * to be clocked against the system clock so that simulation can be observed by
 * the user.  The system does not make a hard real-time guarantee of execution. 
 * It only guarantees that the simulator will execute no faster than the realtimeRate.
 * The maximum rate can be set to POSITIVE_INFINITY for fastest execution, but the
 * true maximum speed is limited by CPU speed.
 *
 * Simultaneous events are permuted randomly (according to the RandomSource passed
 * in constructor).  This models jitter in real systems and keeps periodic events that
 * are synchronized (occur at the same time every period) from always executing
 * in the same order.
 *
 * The event list also supports non-simulation events.  The same event object
 * (FutureEvent) is used, but they are scheduled in a separate queue.  Non-simulation
 * events are executed before any simulation events that occur at the same time.
 * Non-simulation events are executed in such a way that they do not affect the
 * random permutation of simulation events.
 *
 * Non-simulation events are used to implement breakpoints, which halt the simulation
 * at a certain time.  See SwingDisplay (which is used to restart the simulation
 * and control the realtime rate) and SystemTimer, which also uses non-simulation
 * events.
 *
 *
 *
 */
public class FutureEventList {

    /**
     * Data structure class for executing events in time order.  This implementation
     * uses a TreeMap of Lists data structure.  Each list is associated with a SimTime
     * that represents the time when that event is to occur, and the list contains
     * all events sheduled for that time.  The TreeMap provides total ordering of events.
     * 
     * This implementation is prefered over a PriorityQueue because testing shows
     * that there are many sets of events that occur simulataneously.  Since we have
     * to pull all these events from the queue, put them in a list, permute them, then
     * execute them, it is (slightly) faster to store them in a list to begin with.
     * 
     * @param <T> Generic type for the data the queue is to hold.
     */
    private class ArrayQueue<T> {

        private TreeMap<SimTime, List<T>> events = new TreeMap<SimTime, List<T>>();

        public void addItem(SimTime when, T item) {
            List<T> itemList = events.get(when);
            if (itemList == null) {
                itemList = new LinkedList<T>();
                itemList.add(item);
                events.put(when, itemList);
            } else {
                //add the item to the list for that time.
                //Don't need to put() into the map because the list is already there.
                itemList.add(item);
            }
        }

        public SimTime peek() {
            return events.firstKey();
        }

        public List<T> getNextList() {
            return events.pollFirstEntry().getValue();
        }

        public boolean isEmpty() {
            return events.isEmpty();
        }
    }
    
    //private final Queue<FutureEvent> futureEvents;
    //private final Queue<FutureEvent> systemEvents;
     /*
     * Data structures to hold the events.  See documentation of ArrayQueue for
      * discussion.  JR 2010-05-22
     */
    private final ArrayQueue<FutureEvent> futureEvents;
    private final ArrayQueue<FutureEvent> systemEvents;
    /** Current simulation time */
    private SimTime wallClock;
    /** Time at which simulation should end */
    private SimTime endTime;
    /**
     * Whether the simulation should end immediately, regardless of
     * <code>endTime</code>.
     */
    private boolean endNow;
    private RandomSource randomSource;
    private double realtimeRate = 1.0;
    private boolean blockFlag = false;
    //breakpoint
    //private final Queue<SimTime> breakpoints;
    private final HashMap<SimTime, Breakpoint> breakpoints;
    private final ArrayList<BreakpointListener> breakpointListeners;

    /** used for stepping through the simulation */
    public FutureEventList(RandomSource randomSource) {
        wallClock = SimTime.ZERO;
        endNow = false;
        endTime = SimTime.FOREVER;

        //futureEvents = new PriorityQueue<FutureEvent>();
        //systemEvents = new PriorityQueue<FutureEvent>();
        futureEvents = new ArrayQueue<FutureEvent>();
        systemEvents = new ArrayQueue<FutureEvent>();

        this.randomSource = randomSource;
        breakpoints = new HashMap<SimTime, Breakpoint>();
        breakpointListeners = new ArrayList<BreakpointListener>();
    }

    /**
     * Halt the simulator at a specified time
     * @param endTime When to stop the simulator.  This should be interpreted as
     * clock time.
     */
    public void setEndTime(SimTime time) {
        endTime = time;
    }

    /**
     * Adjust the speed the simulator runs at.  This is not a hard realtime guarantee,
     * just a guarantee that the simulator will run no faster than the specified rate.
     *
     * The simulator can run as slow as is needed, but to run faster than realtime,
     * the upper limit is based on the performance of the system and the java VM.
     *
     * @param rate  the realtime rate
     */
    public void setRealtimeRate(double rate) {
        if (rate < 0) {
            throw new IllegalArgumentException("negative rate: " + rate);
        }
        realtimeRate = rate;
    }

    /**
     *
     * @return the current realtime rate
     */
    public double getRealtimeRate() {
        return realtimeRate;
    }

    /**
     * Schedules the specified event to happen in the future.  The event will
     * happen <code>timeInterval</code> time ticks into the future.  The specified
     * <code>data</code> will be passed to {@link FutureEventListener#eventReleased(Object)}
     * when the event occurs.
     * 
     * @return
     * A unique object that can be passed to {@link #cancelEvent(Object, boolean)}
     * to identify this event.
     * 
     * @throws IllegalArgumentException
     * if <code>timeInterval</code> is negative
     */
    public FutureEvent schedule(FutureEventListener event, SimTime timeInterval, Object data) {
        if (timeInterval.isNegative()) {
            throw new IllegalArgumentException("negative timeInterval");
        }

        FutureEvent e = new FutureEvent(event,
                SimTime.add(wallClock, timeInterval), data);
        futureEvents.addItem(e.when, e);
        return e;
    }

    /**
     * Works just like scheduleEvent, but puts the event into the system queue (which are executed first and do not 
     * affect the random permutation of other events).
     */
    public FutureEvent scheduleNonsimulationEvent(FutureEventListener event, SimTime timeInterval, Object data) {
        if (timeInterval.isNegative()) {
            throw new IllegalArgumentException("negative timeInterval");
        }

        FutureEvent e = new FutureEvent(event,
                SimTime.add(wallClock, timeInterval), data);
        systemEvents.addItem(e.when, e);
        return e;
    }

    /**
     * Works jsut like cancelEvent but for nonsimulation events.
     * @param event
     * @param deleteNow
     * @return
     */
    public void cancelNonsimulationEvent(FutureEvent event) {
        if (!event.isScheduled()) {
            throw new IllegalArgumentException("event is not scheduled: " + event);
        }
        event.expire();
    }

    /**
     * Cancel the event associated with <code>label</code>. If the
     * event's release time coincides with the current simulation time,
     * the event will be removed if and only if <code>deleteNow</code>
     * is <code>true</code>.
     */
    public void cancelEvent(FutureEvent event) {
        if (!event.isScheduled()) {
            throw new IllegalArgumentException("event is not scheduled: " + event);
        }
        event.expire();
    }

    /**
     * Start the simulator
     * @param howLong how long to run the simulator.  This value should be interpreted as an
     * interval.
     */
    public void runSimulationUntil(SimTime end) {
        endTime = end;
        runSimulation();
    }

    /**
     * Drives the simulation one step at a time until there are no more events
     * to be released, the simulation time exceeds the specified ending time, or
     * the simulation is ended immediately by calling {@link #endSimulation()}.
     */
    public void runSimulation() {
        while (!endNow && executeSimulationStep()) {
            // empty loop
        }
    }

    /**
     * Advance the simulation to the time of the next event and execute all
     * events at that time.  Also executes system events that occur before (or
     * at the same time as) the next future event time.
     * 
     * @return <code>true</code> if there are other events that have yet to be
     * released.
     */
    private boolean executeSimulationStep() {
        interleaveLock.lock();
        try {
            if (futureEvents.isEmpty()) {
                return false;
            }

            SimTime nextEventTime = futureEvents.peek();

            //long nextEventNS = nextEventTime.getTruncNanoseconds();
            //nextEventNS = nextEventNS - (Math.round((double) nextEventNS / 1000.0) * 1000);
            // if (nextEventNS != 0) {
            //     System.out.println("Noteworthy event:"  + nextEventTime);
            // }

            if (nextEventTime.isGreaterThan(endTime)) {
                return false;
            }

            //execute any pending system events
            SimTime nextSystemEventTime = systemEvents.peek();
            while (!systemEvents.isEmpty() && nextSystemEventTime.isLessThanOrEqual(nextEventTime)) {
                List<FutureEvent> nextSystemEvents = systemEvents.getNextList();

                waitUntil(nextSystemEventTime);

                wallClock = nextSystemEventTime;

                for (FutureEvent e : nextSystemEvents) {
                    if (e.isScheduled()) {
                        e.expire();
                        e.handler.eventReleased(e.callback);
                    }
                }
                nextSystemEventTime = systemEvents.peek();
            }

            //wait for next non-system event time
            waitUntil(nextEventTime);

            wallClock = nextEventTime;

            //pull all the simultaneous events from the queue
            //List<FutureEvent> eventBatch = new LinkedList<FutureEvent>();
            //while (!futureEvents.isEmpty() && futureEvents.peek().when.equals(nextEventTime)) {
            //eventBatch.add(futureEvents.remove());
            //}
            List<FutureEvent> eventBatch = futureEvents.getNextList();

            //shuffle the events
            Collections.shuffle(eventBatch, randomSource.getRandom());

            //JR 2010-05-21
            //even though this call has no effect, it provides a noticeable speeds up in the simulator
            //probably a side-effect of this call is some cleanup on the internal data structtres that 
            //makes the enumeration that follows faster
            eventBatch.size();

            for (FutureEvent e : eventBatch) {
                if (e.isScheduled()) {
                    e.expire();
                    e.handler.eventReleased(e.callback);
                }
            }
            //clear the event list
            //eventBatch.clear();
        } finally {
            interleaveLock.unlock();
        }
        return !futureEvents.isEmpty();
        //return true;
    }

    /**
     * Halt the simulator
     */
    public void endSimulation() {
        endNow = true;


    }

    /**
     * Utility method that clocks the simulator against the system clock using
     * the current realtime rate.
     * @param targetTime
     */
    private void waitUntil(SimTime targetTime) {
        //compute the offest from start time in microseconds, multiply it by the realtime rate
        //the subtract from the target time to get the wait time
        //long start, end;
        //start = System.nanoTime();
        if (realtimeRate == Double.POSITIVE_INFINITY) {
            return;
        }

        if (targetTime.equals(wallClock)) {
            return;
        }

        if (realtimeRate == 0) {
            blockSimulation();


            return;


        } //System.out.println("waitUntil:targetTime  " + targetTime);
        //compute the amount of real time to wait based on the offset between the 
        long realtimeOffsetMs = (long) ((targetTime.getFracMilliseconds() - wallClock.getFracMilliseconds()) / realtimeRate);
        //System.out.println("realtimeOffsetMs = " + realtimeOffsetMs);


        if (realtimeOffsetMs > 0) {
            //use sleep to wait
            try {
                Thread.sleep(realtimeOffsetMs);


            } catch (InterruptedException ex) {
                //do nothing on interrupt
            }
        }
    }

    /**
     * Halt the simulation temporarily -- used for breakpoints and rate adjustments.
     */
    private synchronized void blockSimulation() {
        blockFlag = true;


        while (blockFlag) {
            try {
                wait();


            } catch (InterruptedException ex) {
                //do nothing on interrupt
            }
        }
    }

    /**
     * Move the simulation forward to the next event instant.
     */
    public synchronized void stepSimulation() {
        blockFlag = false;
        notifyAll();


    }

    /**
     * Add a breakpoint at the specified time.  The breakpoint stops execution
     * before any events that occur at that time.
     * 
     * @throws IllegalArgumentException if the specified time is in the past
     * or at the current simulation time
     *        
     * @param breakpointTime the absolute time at which to pause execution
     *
     * @return <code>true</code> if the breakpoint was added,
     * <code>false</code> if there was already a breakpoint at that time.
     */
    public boolean addBreakpoint(SimTime breakpointTime) {
        if (breakpoints.containsKey(breakpointTime)) {
            return false;


        }
        breakpoints.put(breakpointTime, new Breakpoint(breakpointTime));


        return true;


    }

    /**
     * Remove a breakpoint
     * @param breakpointTime
     * @return
     */
    public boolean removeBreakpoint(SimTime breakpointTime) {
        if (!breakpoints.containsKey(breakpointTime)) {
            return false;


        }
        //breakpoint exists so cancel the event and remove it from the list
        Breakpoint bp = breakpoints.remove(breakpointTime);
        cancelNonsimulationEvent(bp.breakpointEvent);


        return true;








    }

    /**
     * Helper class for implementing breakpoints.  This class notifies the 
     * breakpoint listeners when a breakpoint occurs.
     */
    private class Breakpoint implements FutureEventListener {

        public final FutureEvent breakpointEvent;

        public Breakpoint(SimTime breakpointTime) {
            if (breakpointTime.isLessThanOrEqual(wallClock)) {
                throw new IllegalArgumentException("breakpoint time is not in the future: " + breakpointTime);
            }
            breakpointEvent = new FutureEvent(this, breakpointTime, null);
            systemEvents.addItem(breakpointEvent.when, breakpointEvent);
        }

        public void eventReleased(Object data) {
            //stop simulation
            setRealtimeRate(0.0);
            //notify the listeners
            for (BreakpointListener l : breakpointListeners) {
                l.breakpointOccured(wallClock);
            }
            breakpoints.remove(breakpointEvent.when);
        }
    }

    /**
     * Add an object that can respond when a breakpoint occurs.
     * @param l  The object that gets a callback when the breakpoint happens
     */
    public void addBreakpointListener(BreakpointListener l) {
        breakpointListeners.add(l);


    }

    /**
     * stop listeneing to breakpoint callbacks.
     * @param l
     * @return
     */
    public boolean removeBreakpointListener(BreakpointListener l) {
        return breakpointListeners.remove(l);


    }

    /**
     *
     * @return true if the simulation is temporarily halted.
     */
    public boolean isBlocked() {
        return blockFlag;


    }

    /**
     * 
     * @return The current simulation time
     */
    public SimTime getWallClock() {
        return wallClock;


    }

    private void log(Object... msg) {
        simulator.framework.Harness.log("FutureEventList", msg);


    }

//    public String getEventList() {
//
//        StringBuffer sb = new StringBuffer("Wall Clock: " + wallClock + "\n");
//
//
//        if (futureEvents.size() > 0) {
//            sb.append("Event List:\n");
//
//            FutureEvent[] eventList = (FutureEvent[]) futureEvents.toArray();
//
//
//            for (FutureEvent e : eventList) {
//                sb.append(e.toString() + "\n");
//
//
//            }
//        } else {
//            sb.append("No events on Future Event List\n");
//
//
//        }
//        return sb.toString();
//
//
//    }
    public static void test1() {
        FutureEventList fel = new FutureEventList(new RandomSource());
        //fel.setRealtimeRate(Double.POSITIVE_INFINITY);


        for (int i = 0; i
                < 10; i++) {
            SimTime startTime = new SimTime(i, SimTime.SimTimeUnit.SECOND);
            PrintEvent p = new PrintEvent("Event " + i + " at " + startTime);
            Object label = fel.schedule(p, startTime, p);


            if (i == 3) {
                // fel.cancelEvent(label, true);
            }
        }
        fel.runSimulationUntil(new SimTime(20, SimTime.SimTimeUnit.SECOND));


    }

    public static void test2() {
        FutureEventList fel = new FutureEventList(new RandomSource());
        //fel.setRealtimeRate(Double.POSITIVE_INFINITY);


        for (int i = 0; i
                < 10; i++) {
            SimTime startTime = new SimTime(i, SimTime.SimTimeUnit.MILLISECOND);
            PrintEvent p = new PrintEvent("Event " + i + " at " + startTime);
            Object label = fel.schedule(p, startTime, p);


            if (i == 3) {
                // fel.cancelEvent(label, true);
            }
        }
        fel.runSimulationUntil(new SimTime(20, SimTime.SimTimeUnit.SECOND));


    }

    public static void test3() {
        final FutureEventList fel = new FutureEventList(new RandomSource());
        //fel.setRealtimeRate(Double.POSITIVE_INFINITY);


        double[] speeds = {
            0.1,
            0.5,
            1.0,
            2.0,
            10.0,
            100.0,
            1000.0,
            10000.0,
            Double.POSITIVE_INFINITY
        };


        int timeOffset = 1;


        for (Double speed : speeds) {
            SimTime startTime = new SimTime(timeOffset, SimTime.SimTimeUnit.SECOND);
            final double speed_ = speed;
            FutureEventListener speedSetter = new FutureEventListener() {

                public void eventReleased(Object data) {
                    System.out.println("Setting speed to " + speed_);
                    fel.setRealtimeRate(speed_);


                }
            };
            fel.schedule(speedSetter, startTime, null);
            timeOffset++;

        }


        SimTime startTime = new SimTime(timeOffset, SimTime.SimTimeUnit.SECOND);
        PrintEvent p = new PrintEvent("Final Event");
        fel.schedule(p, startTime, p);

        fel.runSimulationUntil(new SimTime(40, SimTime.SimTimeUnit.SECOND));


    }

    public static void breakpointTest() {
        final FutureEventList fel = new FutureEventList(new RandomSource());
        fel.setRealtimeRate(1.0);
        //register the print listener
        fel.addBreakpointListener(new BreakpointPrinter(System.out));
        //register a listener that resets the rate to 1.0
        fel.addBreakpointListener(new BreakpointListener() {

            public void breakpointOccured(SimTime breakpointTime) {
                System.out.println("Resetting runtime rate to 1.0 at time " + fel.getWallClock());
                fel.setRealtimeRate(1.0);


            }
        });

        //register some breakpoints
        fel.addBreakpoint(new SimTime(1, SimTimeUnit.SECOND));
        fel.addBreakpoint(new SimTime(2500, SimTimeUnit.MILLISECOND));
        fel.addBreakpoint(new SimTime(8500, SimTimeUnit.MILLISECOND));
        fel.addBreakpoint(new SimTime(8505, SimTimeUnit.MILLISECOND));
        //this breakpoint should not execute because it is after the last event
        fel.addBreakpoint(new SimTime(10, SimTimeUnit.SECOND));




        for (int i = 0; i
                < 10; i++) {
            SimTime startTime = new SimTime(i, SimTime.SimTimeUnit.SECOND);
            PrintEvent p = new PrintEvent("Event " + i + " at " + startTime);
            fel.schedule(p, startTime, p);


        }
        fel.runSimulationUntil(new SimTime(20, SimTime.SimTimeUnit.SECOND));


    }

    public static void sleepTest() {
        long start, end;


        double elapsed;


        for (int i = 0; i
                < 50; i += 5) {
            start = System.nanoTime();


            try {
                Thread.sleep(i);


            } catch (InterruptedException ex) {
                //do nothing
            }
            end = System.nanoTime();
            elapsed = (end - start) / 1000000;
            System.out.println("Requested " + i + " ms.  Actual = " + elapsed + " ms.");


        }

        for (int i = 0; i
                < 2000; i += 100) {
            start = System.nanoTime();


            try {
                Thread.sleep(0, i);


            } catch (InterruptedException ex) {
                //do nothing
            }
            end = System.nanoTime();
            elapsed = (end - start);
            System.out.println("Requested " + i + " ns.  Actual = " + elapsed + " ns.");


        }
    }

    /*public static void test2() {
    FutureEventList fel = new FutureEventList();
    for (int i = 0; i < 50; ++i) {
    long when = Math.round(Long.MAX_VALUE * Math.random());
    fel.schedule(new PrintEvent("" + when), when, null);
    }
    fel.runSimulation();
    }*/
    public static void main(String[] Args) {
        //test1();
        //System.out.println("\n****************************************\n");
        //test2();
        //test3();
        breakpointTest();
        //sleepTest();


    }
    private final ReentrantLock interleaveLock = new ReentrantLock(true);

    /**
     * call to acquire a lock on the event queue.  While locked, the simulator
     * will not step forward.  This allows other threads (mainly the gui threads)
     * to syncronize with the main simulation thread.
     */
    public void interleaveLock() {
        interleaveLock.lock();


    }

    /**
     * Release the lock on the event queue.
     */
    public void interleaveUnlock() {
        interleaveLock.unlock();

    }
}
