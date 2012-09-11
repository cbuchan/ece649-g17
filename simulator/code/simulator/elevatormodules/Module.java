package simulator.elevatormodules;

import jSimPack.SimTime;
import simulator.payloads.CANNetwork.CanConnection;
import simulator.framework.Harness;
import simulator.payloads.NetworkScheduler.Connection;
import simulator.payloads.Networkable;

/**
 * Utility superclass for system modules.
 * @author Justin Ray
 */
public abstract class Module extends Networkable {

    final protected CanConnection canNetworkConnection;
    final protected Connection physicalConnection;
    protected SimTime period;
    protected final String name;
    private final boolean verbose;

    public Module(SimTime period, String name, boolean verbose) {
        this.period = period;
        this.name = name;
        canNetworkConnection = Harness.getCANNetwork().getCanConnection();
        physicalConnection = Harness.getPhysicalNetwork().getFrameworkConnection(this);
        this.verbose = verbose;
    }

    protected void log(Object... msg) {
        if(verbose) Harness.log(name, msg);
    }

    @Override
    public String toString() {
        return name;
    }
}