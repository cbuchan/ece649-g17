/**
 * Generates fault messages that are read from a file.
 * 
 * @author Charles Shelton
 * @author Kenny Stauffer
 */
package simulator.framework;

import simulator.payloads.Networkable;
import jSimPack.SimTime;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;
import simulator.framework.faults.Fault;

/**
 * Responsible for creating and injecting fault objects as defined by the fault
 * file (-ff).
 * @author justinr2
 */
public class FaultInjector extends Networkable implements TimeSensitive, Parser {

    private final boolean verbose;
    private ArrayList<Fault> faults = new ArrayList<Fault>();
    private SystemTimer sysTimer = new SystemTimer(this);
    private final static SimTime STATS_INTERVAL = new SimTime(1, SimTime.SimTimeUnit.MINUTE);

    public FaultInjector(String filename, boolean verbose) {
        this.verbose = verbose;
        //refFactory = new ReflectionFactory(verbose);
        //fi = Harness.getPhysicalNetwork().getFrameworkConnection(this);
        FileTokenizer ft = new FileTokenizer(filename, verbose, this);
        ft.parseFile();
        sysTimer.start(STATS_INTERVAL);
    }

    public void parse(String[] words, FileTokenizer sourceFT) throws ParseException {
        logPrint(sourceFT.lineMessage("parsing " + Arrays.toString(words)));

        if (words.length < 1) {
            throw new ParseException(logMessage("received " + words.length +
                    "parameters, expected at least one (type)"), 0);
        }

        String faultName = "simulator.framework.faults." + words[0] + "Fault";
        String[] faultArgs = new String[words.length-1];
        for (int i=1; i < words.length; i++) {
          faultArgs[i-1] = words[i];
        }

        //create the class 
        Fault fault = null;
        try {
            Class<?> faultClass = Class.forName(faultName);
            Constructor<?> con = faultClass.getConstructor(String[].class);
            fault = (Fault)con.newInstance((Object)faultArgs);
        } catch (InstantiationException ex) {
            throw new ParseException(sourceFT.lineMessage("Error constructing " + faultName + ": " + ex.toString()), 0);
        } catch (IllegalAccessException ex) {
            throw new ParseException(sourceFT.lineMessage("Error constructing " + faultName + ": " + ex.toString()), 0);
        } catch (IllegalArgumentException ex) {
            throw new ParseException(sourceFT.lineMessage("Error constructing " + faultName + ": " + ex.toString()), 0);
        } catch (InvocationTargetException ex) {
            throw new ParseException(sourceFT.lineMessage("Error constructing " + faultName + ": " + ex.getCause().toString()), 0);
        } catch (ClassNotFoundException ex) {
            throw new ParseException(sourceFT.lineMessage("Could not find class " + faultName + ": " + ex.toString()), 0);
        } catch (NoSuchMethodException ex) {
            throw new ParseException(sourceFT.lineMessage("Could not find string array constructor for " + faultName + ": " + ex.toString()), 0);
        } catch (SecurityException ex) {
            throw new ParseException(sourceFT.lineMessage("Security Exception parsing " + faultName + ": " + ex.toString()), 0);
        }

        logPrint("Injecting Fault " + fault.toString());
        fault.setVerbose(verbose);
        faults.add(fault);
    }

    private String logMessage(String s) {
        return "FaultInjector: @TIME " + Harness.getTime() + ": " + s;
    }

    private void logPrint(String s) {
        if (verbose) {
            Harness.log("FaultInjector", s);
        }
    }
    
    public String getStats() {
        StringBuffer sb = new StringBuffer("Fault Injection Stats:\n");
        for (Fault f : faults) {
            sb.append(f.getFaultStats() + "\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SimTime simRunTime = new SimTime(7, SimTime.SimTimeUnit.SECOND);
        new FaultInjector("tests-new/fault/basicfault.fault", true);
        System.out.println("--> Starting Simulation");
        Harness.runSim(simRunTime);
        System.out.println("--> Simulation Finished");
    }

    public void timerExpired(Object callbackData) {
        logPrint(getStats());
        sysTimer.start(STATS_INTERVAL);
    }
}
