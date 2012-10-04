package jSimPack;

import java.io.PrintStream;

/**
 * Utility class that prints a message when a breakpoint occurs.
 * @author Justin Ray
 */
public class BreakpointPrinter implements BreakpointListener {

    PrintStream p;

    /**
     *
     * @param p The print stream the message will be printed to.
     */
    public BreakpointPrinter(PrintStream p) {
        this.p = p;
    }
    
    public void breakpointOccured(SimTime breakpointTime) {
        p.println("Breakpoint occured at " + breakpointTime);
    }

}
