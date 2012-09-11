/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import simulator.payloads.OverrideMessageFaultModel;
import simulator.framework.faultmodels.*;
import jSimPack.SimTime;
import java.text.ParseException;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Direction;
import simulator.framework.Elevator;
import simulator.framework.Hallway;
import simulator.framework.Harness;
import simulator.payloads.NetworkScheduler;
import simulator.framework.ReplicationComputer;
import simulator.payloads.CanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.CarCallPayload;
import simulator.payloads.CarCallPayload.WriteableCarCallPayload;
import simulator.payloads.CarLightPayload;
import simulator.payloads.CarLightPayload.WriteableCarLightPayload;
import simulator.payloads.HallCallPayload;
import simulator.payloads.HallCallPayload.WriteableHallCallPayload;
import simulator.payloads.HallLightPayload;
import simulator.payloads.HallLightPayload.WriteableHallLightPayload;
import simulator.payloads.PhysicalPayload.PhysicalWriteablePayload;

/**
 * This fault model implements a fail-silent button failure.
 * Fail-silent means that the physical outputs (lights) are turned off and
 * all network messages are blocked.
 * @author justinr2
 */
public class FailedButtonFault extends Fault {

    /**
     * Constants used to specify car or hall button faults.
     */
    public enum ButtonFaultType {

        CarCall,
        HallCall
    }
    public NetworkScheduler ns = null;
    private ButtonFaultType type;
    private int floor;
    private Hallway hallway;
    private Direction direction;
    PhysicalWriteablePayload lightPayload;
    PhysicalWriteablePayload callPayload;
    WriteableCanMailbox networkCallMessage;
    WriteableCanMailbox networkLightMessage;
    BlockMessageFaultModel networkCallFault;
    BlockMessageFaultModel networkLightFault;
    OverrideMessageFaultModel frameworkCallFault;
    OverrideMessageFaultModel frameworkLightFault;

    /**
     * Expects at least 5 argumnets:
     * -start time
     * -duration
     * -button type (ButtonTypeFault value)
     * -floor the floor of the faulted button
     * -hall the hallway of the faulted button
     * -direction the direction, only if the button type was HallCall
     * 
     * @param args
     * @throws ParseException
     */
    public FailedButtonFault(String[] args) throws ParseException {
        SimTime startTime;
        SimTime duration;

        //parse the arguments
        if (args.length < 5) {
            throw new ParseException("FailedButtonFault requires at least 5 arguments", 0);
        }
        try {
            startTime = new SimTime(args[0]);
            duration = new SimTime(args[1]);
        } catch (NumberFormatException ex) {
            throw new ParseException("FailedButtonFault:  " + ex.getMessage(), 0);
        }
        try {
            type = ButtonFaultType.valueOf(args[2]);
        } catch (IllegalArgumentException ex) {
            throw new ParseException("FailedButtonFault:  no fault of type " + args[2], 0);
        }
        try {
            floor = Integer.parseInt(args[3]);
            if (floor < 1 || floor > Elevator.numFloors) {
                throw new ParseException("FailedButtonFault:  Floor " + floor + " is out of the range [1," + Elevator.numFloors + "]", 0);
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("FailedButtonFault:  Could not parse " + args[3] + " into an integer", 0);
        }

        try {
            hallway = Hallway.valueOf(args[4]);
            if (hallway != Hallway.FRONT && hallway != Hallway.BACK) {
                throw new ParseException("FailedButtonFault:  Hallway " + hallway + " is must be FRONT or BACK", 0);
            }
        } catch (IllegalArgumentException ex) {
            throw new ParseException("FailedButtonFault:  Hallway " + args[4] + " is not a valid type", 0);
        }
        if (type == ButtonFaultType.HallCall) {
            if (args.length < 6) {
                throw new ParseException("FailedButtonFault requires at least 6 arguments for HallCalls", 0);
            }
            try {
                direction = Direction.valueOf(args[5]);
                if (direction != Direction.DOWN && direction != Direction.UP) {
                    throw new ParseException("FailedButtonFault:  Direction " + direction + " is must be UP or DOWN", 0);
                }
            } catch (IllegalArgumentException ex) {
                throw new ParseException("FailedButtonFault:  Direction " + args[5] + " is not a valid type", 0);
            }
        }
        //now create payloads based on the arguments
        switch (type) {
            case HallCall:
                /*    PhysicalPayload lightPayload;
                PhysicalPayload callPayload;
                CanMailbox networkCallMessage;
                CanMailbox networkLightMessage;*/
                setName("FailedButtonFault-HallCall[" + floor + "," + hallway + "," + direction + "]");
                //create objects for hall call
                WriteableHallLightPayload hp = HallLightPayload.getWriteablePayload(floor, hallway, direction);
                hp.set(false);
                lightPayload = hp;

                WriteableHallCallPayload hc = HallCallPayload.getWriteablePayload(floor, hallway, direction);
                hc.set(false);
                callPayload = hc;

                networkCallMessage = CanMailbox.getWriteableCanMailbox(MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, direction));
                networkLightMessage = CanMailbox.getWriteableCanMailbox(MessageDictionary.HALL_LIGHT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway, direction));
                break;
            case CarCall:
                setName("FailedButtonFault-CarCall[" + floor + "," + hallway + "]");

                WriteableCarLightPayload cp = CarLightPayload.getWriteablePayload(floor, hallway);
                cp.set(false);
                lightPayload = cp;

                WriteableCarCallPayload cc = CarCallPayload.getWriteablePayload(floor, hallway);
                cc.set(false);
                callPayload = cc;

                networkCallMessage = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));
                networkLightMessage = CanMailbox.getWriteableCanMailbox(MessageDictionary.CAR_LIGHT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, hallway));
                break;
            default:
                throw new RuntimeException("Invalid type " + type);
        }

        log("Creating failed button ",this.name," from ",startTime, " for duration ",duration);
        networkCallFault = new BlockMessageFaultModel(networkCallMessage, startTime, duration, false);
        networkLightFault = new BlockMessageFaultModel(networkLightMessage, startTime, duration, false);
        frameworkCallFault = new OverrideMessageFaultModel(callPayload, startTime, duration);
        frameworkLightFault = new OverrideMessageFaultModel(lightPayload, startTime, duration);

        //create and register fault models
        Harness.getCANNetwork().registerFaultModel(networkCallFault);
        Harness.getCANNetwork().registerFaultModel(networkLightFault);
        Harness.getPhysicalNetwork().registerFaultModel(frameworkCallFault);
        Harness.getPhysicalNetwork().registerFaultModel(frameworkLightFault);
    }

    @Override
    public void setVerbose(boolean verbose) {
        super.setVerbose(verbose);
        networkCallFault.setVerbose(verbose);
        networkLightFault.setVerbose(verbose);
        frameworkCallFault.setVerbose(verbose);
        frameworkLightFault.setVerbose(verbose);
    }
    
}
