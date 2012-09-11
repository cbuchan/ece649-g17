/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework;

import java.util.HashMap;
import java.util.Map;
import simulator.payloads.Networkable;
import simulator.payloads.CANNetwork.CanConnection;
import simulator.payloads.PhysicalNetwork.PhysicalConnection;

/**
 * The Controller object provides an abstract framework that you will use to
 * build your distributed elevator controllers.  The main features this class
 * provides are:
 * -network and physical message interfaces
 * -timer callbacks for control loops (see Time and AbstractTimer for details)
 * -an efficient logging method
 * -a state reporting mechanism for use with state assertions in unit and
 *  integration testing.
 *
 * Note that you MUST use the logging mechanism for any debug output.  The controller
 * code you submit may not print anything to System.out or System.err if the verbose
 * parameter is set to false!  This helps us avoid huge log files when we run
 * your tests.
 *
 * @author Justin Ray
 */
public abstract class Controller extends Networkable implements TimeSensitive {

    public final String STATE_KEY = "STATE";  //string to use as key for the current state of the controller.
    protected final CanConnection canInterface = Harness.getCANNetwork().getCanConnection();
    protected final PhysicalConnection physicalInterface = Harness.getPhysicalNetwork().getConnection();
    protected final Timer timer = new Timer(this);
    private Map<String, String> stateValues = new HashMap<String, String>();
    
    protected boolean verbose = false;
    protected final String name;

    
    /**
     * Superclass constructor.
     * 
     * Important note:  the name for all controllers (including replicated instances)
     * must be different.  It is suggested you use a bracket notation like:
     * DoorControl[FRONT][LEFT] or HallButtonControl[3][FRONT][UP].
     * 
     * simulator.framework.ReplicationComputer has utility methods to assist
     * in the generation of these strings.
     * 
     * @param name Controller name.  This is the name that is referenced by 
     * state assertions in simlator.framework.MessageInjector.
     * @param verbose if true, all messages that use the log() method will be
     * printed to the commandline.
     */
    public Controller(String name, boolean verbose) {
        this.verbose = verbose;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return The name of the controller.  This name is used to identify the 
     * controller for state assertions.
     */
    public final String getName() {
        return name;
    }

    /**
     * This method is called by the message injector to check state assertions.
     * Check the commandline documentation for the syntax of state assertions
     * in the .mf file.
     *
     * State assertions depend on you correctly implementing calls to setState(),
     * e.g. correctly reporting the current state).
     *
     * @see setState()
     *
     * @return the current set of state values.
     * @throws IllegalArgumentException if the key does not exist.
     */
    public final String checkState(String key) {
        if (!stateValues.containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " not found in state values.");
        }
        return stateValues.get(key);
    }

    /**
     * Check to see if a state key exists without causing an exception
     * @param key
     * @return true if an entry with the given key exists, false otherwise
     */
    public final boolean hasState(String key) {
        return stateValues.containsKey(key);
    }


    /**
     * Subclasses should call this method to update the values returned by
     * checkState().
     *
     *
     * @param values  new state values
     */
    protected final void setState(String key, String value) {
        if (key == null) throw new NullPointerException("key");
        if (value == null) throw new NullPointerException("value");
        log("State stored: ",key,"<-",value);
        stateValues.put(key, value);
    }
   
    /**
     * Call this method to log information to the command line.  It will automatically
     * print the current simulation time and the name of the controller.
     *
     * This method accepts a variable number of arguments which are converted
     * to strings.  For perfomance reasons, if you need to build a log line,
     * you should pass the line items as arguments, not as a concatenated string.
     *
     * the wrong way:
     * log(myStatus.toString() + " - " + otherInfo + " - " + errorState);
     * the right way:
     * log(myStatus.toString()," - ",otherInfo," - ",errorState);
     *
     * The reason for this distinction is that the wrong way forces the compiler
     * to actually construct the string regardless of whether or not the line is
     * actually printed.  For complex objects, toString calls can be expensive.
     * The latter example avoids the overhead of calling toString and constructing
     * the log line unless logging is enabled.
     *
     * @param msg variable number of objects to be logged
     */
    protected void log(Object... msg) {
        if (!verbose) {
            return;
        }
        Harness.log(name, msg);
    }
}
