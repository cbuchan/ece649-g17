package jSimPack;

/**
 * Implement this class to register and respond to breakpoints in the 
 * future event list.
 * @author Justin Ray
 */
public interface BreakpointListener {
    /**
     * This method is called when a breakpoint occurs.
     * @param breakpointTime The time (simulation time) of the registered breakpoint.
     */
    public void breakpointOccured(SimTime breakpointTime);
}
