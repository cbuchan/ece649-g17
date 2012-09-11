/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import simulator.framework.faultmodels.*;
import jSimPack.SimTime;
import java.text.ParseException;
import simulator.framework.DoorCommand;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.payloads.NetworkConnection;
import simulator.payloads.Networkable;
import simulator.framework.ReplicationComputer;
import simulator.framework.Side;
import simulator.payloads.DoorMotorPayload;
import simulator.payloads.DoorMotorPayload.ReadableDoorMotorPayload;

/**
 * Black out the network for a certain period of time after the door motors start.
 *
 * @author Justin Ray
 */
public class DoorMotorBlackoutFault extends Fault {

    
    private ReadableDoorMotorPayload doorMotors[] = new ReadableDoorMotorPayload[4];
    private boolean wasStopped[] = new boolean[4];
    private SimTime blackoutDuration;
    private NetworkConnection pconn;
    private BlackoutFaultModel blackout;

    /**
     * Expects one argument which is converted to a SimTime value.
     * @param args
     * @throws ParseException if the argument is invalid.
     */
    public DoorMotorBlackoutFault(String[] args) throws ParseException {
        this();
        if (args.length != 1) {
            throw new ParseException("DoorMotorFaultModel expects 1 argument, found " + args.length + " arguments", 0);
        }
        try {
            blackoutDuration = new SimTime(args[0]);
        } catch (NumberFormatException ex) {
            throw new ParseException("DoorMotorFaultModel;  cannot parse " + args[0] + " into a time.", 0);
        }
        
    }

    public DoorMotorBlackoutFault() {
        super("DoorMotorBlackoutFault");
        pconn = Harness.getPhysicalNetwork().getFrameworkConnection(new DoorMotorMonitor());
        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                int index = ReplicationComputer.computeReplicationId(h, s);
                doorMotors[index] = DoorMotorPayload.getReadablePayload(h, s);
                pconn.registerEventTriggered(doorMotors[index]);
                wasStopped[index] = true;
            }
        }
        //create and register the blackout fault model
        blackout = new BlackoutFaultModel(verbose);
        Harness.getCANNetwork().registerFaultModel(blackout);
        log("Fault created");
    }

    @Override
    public void setVerbose(boolean verbose) {
        super.setVerbose(verbose);
        blackout.setVerbose(verbose);
    }
    
    

    private class DoorMotorMonitor extends Networkable {
        //react to door motor payload messages
        @Override
        public void receive(ReadableDoorMotorPayload msg) {
            int index = ReplicationComputer.computeReplicationId(msg.getHallway(), msg.getSide());
            if (msg.command() != DoorCommand.STOP && wasStopped[index] == true) {
                //stopped and now now not stopped, so trigger the fault
                wasStopped[index] = false;
                blackout.blackoutStart(blackoutDuration);
            } else if (msg.command() == DoorCommand.STOP) {
                wasStopped[index] = true;
            }
        }
    }

    @Override
    public String getFaultStats() {
        return name + ": " + blackout.getStats();
    }
}
