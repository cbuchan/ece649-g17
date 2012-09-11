/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework;

/**
 * Utility class for logging functionality
 * @author justinr2
 */
public class Logger {
    protected boolean verbose = false;
    protected String name = "Fault";
    
    public Logger(String name) {
        this.name = name;
    }
    public Logger(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    
    @Override
    public String toString() {
        return name;
    }
    
    protected void setName(String name) {
        this.name = name;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    protected void log(Object... msg) {
        if (verbose) {
            Harness.log(name, msg);
        }
    }

}
