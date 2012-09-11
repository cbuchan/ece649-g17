/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatormodules.passengers;

import jSimPack.SimTime;
import simulator.framework.Harness;
import simulator.framework.TimeSensitive;
import simulator.framework.Timer;

/**
 * Utility class for actions carried out by the passenger.  An action is a single-use
 * class, and cannot execute twice.
 *
 * @author Justin Ray
 */
public abstract class PassengerAction implements TimeSensitive {

    private SimTime executionTime = null;
    private boolean isCanceled = false;
    private final Timer timer;

    /**
     * Automatically schedules the action to execute after offset
     * @param offset amount of time to wait before executing.
     */
    PassengerAction(SimTime offset) {
        timer = new Timer(this);
        executionTime = SimTime.add(Harness.getTime(), offset);
        //Harness.log("PassengerAction", "now=",Harness.getTime(), " offset=", offset, " execTime=", executionTime);
        timer.start(offset);
    }

    /**
     * callback to execute the action
     * @param callbackData
     */
    public final void timerExpired(Object callbackData) {
        if (isCanceled) return;
        executionTime = Harness.getTime();
        execute();
    }

    /**
     * Actions should implement this method which is called when the offset expires.
     */
    public abstract void execute();

    /**
     *
     * @return the wall clock time when the action was executed, or null if it
     * has not executed yet.
     */
    public final SimTime getExecutionTime() {
        return executionTime;
    }

    /**
     * calling this before the action is executed will prevent it from being
     * executed/
     */
    public final void cancel() {
        isCanceled = true;
    }

    /**
     * 
     * @return true if the action was cancelled
     */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     *
     * @return true if the action has already executed.
     */
    public boolean hasExecuted() {
        return executionTime != null;
    }
}
