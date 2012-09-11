package simulator.framework;

import simulator.payloads.CANNetwork;
import simulator.payloads.PhysicalNetwork;
import jSimPack.*;
import java.io.*;

/**
 * Allows the elevator components to interact with each other through a shared
 * event queue.
 *
 * This method provides a lot of public static methods that are used to connect
 * the various system objects.
 *
 * For legacy reasons, a lot of methods in this class are wrappers for
 * jSimPack.FutureEventList methods.
 *
 * @author Christopher Martin
 * @author Kenny Stauffer
 */
public class Harness { //implements FutureEventListener {

    private static final boolean verbose = false;
    private static final RandomSource randomSource;
    private static PhysicalNetwork thePhysicalNetwork;
    private static CANNetwork theCANNetwork;
    /**
     * The HarnessEventListener objects that have registered to receive events
     * of a particular type.
     */
    private static final FutureEventList eventList;
    private static PrintStream logPrinter = System.out;
    private static final Harness theHarness;

    static {
        theHarness = new Harness();
        randomSource = new RandomSource();
        eventList = new FutureEventList(randomSource);
    }

    public static void initialize(SimTime canBitTime,
            boolean networkVerbose, boolean frameworkNetworkVerbose,
            boolean utilizationVerbose, boolean dropVerbose) {
        thePhysicalNetwork = new PhysicalNetwork();
        theCANNetwork = new CANNetwork(canBitTime);
        theCANNetwork.getUtilization().setVerbose(utilizationVerbose);
        theCANNetwork.setVerbose(networkVerbose);
        theCANNetwork.setDropVerbose(dropVerbose);
        thePhysicalNetwork.setVerbose(frameworkNetworkVerbose);
        //thePhysicalNetwork.setDropVerbose(dropVerbose);
    }

    //interface to add and remove network events
    /**
     * See jSimPack.FutureEventList.schedule()
     */
    public static FutureEvent schedule(FutureEventListener event, SimTime timeInterval, Object data) {
        return eventList.schedule(event, timeInterval, data);
    }

    /**
     * See jSimPack.FutureEventList.scheduleNonsimulationEvent()
     */
    public static FutureEvent scheduleNonsimulationEvent(FutureEventListener event, SimTime timeInterval, Object data) {
        return eventList.scheduleNonsimulationEvent(event, timeInterval, data);
    }

    /**
     * See jSimPack.FutureEventList.cancelNonsimulationEvent()
     */
    public static void cancelNonsimulationEvent(FutureEvent event) {
        eventList.cancelNonsimulationEvent(event);
    }

    /**
     * See jSimPack.FutureEventList.cancelEvent()
     */
    public static void cancelEvent(FutureEvent event) {
        eventList.cancelEvent(event);
    }

    /**
     * See jSimPack.FutureEventList.runSimulationUntil()
     */
    public static void runSim(SimTime howLong) {
        eventList.runSimulationUntil(howLong);
    }


    /**
     * See jSimPack.FutureEventList.endSim()
     */
    public static void endSim() {
        eventList.endSimulation();
    }

    /**
     * See jSimPack.FutureEventList.endTime()
     */
    public static void endSimAt(SimTime endTime) {
        eventList.setEndTime(endTime);
    }


    /**
     * See jSimPack.FutureEventList.setRealtimeRate()
     */
    public static void setRealtimeRate(double rate) {
        eventList.setRealtimeRate(rate);
    }


    /**
     * See jSimPack.FutureEventList.getRealtimeRate()
     */
    public static double getRealtimeRate() {
        return eventList.getRealtimeRate();
    }

    /**
     * See jSimPack.FutureEventList.stepSimulation()
     */
    public static void stepSimulation() {
        eventList.stepSimulation();
    }

    /**
     * See jSimPack.FutureEventList.isBlocked()
     */
    public static boolean simulationIsBlocked() {
        return eventList.isBlocked();
    }

    /**
     * See jSimPack.FutureEventList.getWallClock();
     */
    public static SimTime getTime() {
        return eventList.getWallClock();
    }

    /**
     * the randomSource being used by the simulator.
     */
    public static RandomSource getRandomSource() {
        return randomSource;
    }


    /**
     * @param seed  the random seed to use for the simulation.
     */
    public static void setRandomSeed(long seed) {
        randomSource.setSeed(seed);
    }

    /**
     * @return The current random seed
     */
    public static long getRandomSeed() {
        return randomSource.getSeed();
    }

    /**
     * Direct the logger to log output to a file instead of stdout.
     * @param filename target filename
     * @throws FileNotFoundException if the file cannot be created.
     */
    public static void setLogFilename(String filename)
            throws FileNotFoundException {
        logPrinter = new PrintStream(filename);
    }

    /**
     * Prints a log message that includes the source and timestamp of the
     * message.  This is the main logging function that should be called
     * by the rest of the objects in the system.
     *
     * This method accepts a variable number of arguments which are converted
     * to strings.  For perfomance reasons, if you need to build a log line,
     * you should pass the line items as arguments, not as a concatenated string.
     *
     * the wrong way:
     * log("sourcename", myStatus.toString() + " - " + otherInfo + " - " + errorState);
     * the right way:
     * log("sourcename", myStatus.toString()," - ",otherInfo," - ",errorState);
     *
     * The reason for this distinction is that the wrong way forces the compiler
     * to actually construct the string regardless of whether or not the line is
     * actually printed.  For complex objects, toString calls can be expensive.
     * The latter example avoids the overhead of calling toString and constructing
     * the log line unless logging is enabled.
     * @param source
     * The object that generated this log message.
     * 
     * @param msg
     * The log message to print.
     */
    public static void log(String source, Object... msg) {
        logPrinter.format("[%s] @%4.9f: ", source, getTime().getFracSeconds());
        for (Object o : msg) {
            logPrinter.print(o);
        }
        logPrinter.println();
    }

    //breakpoint methods

    /**
     * See jSimPack.FutureEventList.addBreakpoint()
     */
    public static boolean addBreakpoint(SimTime breakpointTime) {
        return eventList.addBreakpoint(breakpointTime);
    }

    /**
     * See jSimPack.FutureEventList.removeBreakpoint()
     */
    public static boolean removeBreakpoint(SimTime breakpointTime) {
        return eventList.removeBreakpoint(breakpointTime);
    }

    /**
     * See jSimPack.FutureEventList.addBreakpointListener()
     */
    public static void addBreakpointListener(BreakpointListener l) {
        eventList.addBreakpointListener(l);
    }

    /**
     * See jSimPack.FutureEventList.removeBreakpointListener()
     */
    public static boolean removeBreakpointListener(BreakpointListener l) {
        return eventList.removeBreakpointListener(l);
    }

    /**
     * See jSimPack.FutureEventList.interleaveLock()
     */
    static void interleaveLock() {
        eventList.interleaveLock();
    }

    /**
     * See jSimPack.FutureEventList.interleaveUnlock()
     */
    static void interleaveUnlock() {
        eventList.interleaveUnlock();
    }
    
    
    /**
     * @return a reference to the physical network used to represent the state
     * of simulated physical objects.
     */
    public static PhysicalNetwork getPhysicalNetwork() {
        return thePhysicalNetwork;
    }

    /**
     *
     * @return a reference to the CAN network used for controller communication
     */
    public static CANNetwork getCANNetwork() {
        return theCANNetwork;
    }

    private Harness() {
    }
}
