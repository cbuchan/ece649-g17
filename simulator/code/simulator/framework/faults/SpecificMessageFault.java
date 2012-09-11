/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import jSimPack.SimTime;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.framework.Harness;
import simulator.framework.MessageContext;
import simulator.framework.ReflectionFactory;
import simulator.framework.ReplicationComputer;
import simulator.framework.faultmodels.BlockMessageFaultModel;
import simulator.payloads.Payload;

/**
 * Supress a particular network or framework message
 * @author justinr2
 */
public class SpecificMessageFault extends Fault {

    private MessageContext context;
    private static ReflectionFactory rf = new ReflectionFactory(false);

    /**
     * Arguments:
     * -start time
     * -duration
     * -drop at delivery (if true), otherwise drop at arrival
     * -context (F or N)
     * -network or physical message parameters
     * 
     * @param args
     * @throws ParseException
     */
    public SpecificMessageFault(String[] args) throws ParseException {
        SimTime startTime;
        SimTime duration;
        boolean dropAtDelivery;
        int index = 0;

        //parse the arguments
        if (args.length < 5) {
            throw new ParseException("SpecificMessageFault requires at least 5 arguments", 0);
        }
        try {
            startTime = new SimTime(args[index]);
            index++;
            duration = new SimTime(args[index]);
            index++;
        } catch (NumberFormatException ex) {
            throw new ParseException("SpecificMessageFault:  " + ex.getMessage(), 0);
        }

        try {
            dropAtDelivery = FaultUtility.parseBoolean(args[index]);
            index++;
        } catch (NumberFormatException ex) {
            throw new ParseException("SpecificMessageFault:  " + ex.getMessage(), 0);
        }

        //get network type
        if (args[index].equalsIgnoreCase("F")) {
            context = MessageContext.PHYSICAL;
        } else if (args[index].equalsIgnoreCase("N")) {
            context = MessageContext.NETWORK;
        } else {
            throw new ParseException("SpecificMessageFault:  no network of type " + args[index], 0);
        }
        index++;

        int messageType;
        switch(context) {
            case NETWORK:
                messageType = getNetworkMessageID(args, index);
                Harness.getCANNetwork().registerFaultModel(new BlockMessageFaultModel(messageType, startTime, duration, dropAtDelivery));
                break;
            case PHYSICAL:
                messageType = getFrameworkMessageID(args, index);
                Harness.getPhysicalNetwork().registerFaultModel(new BlockMessageFaultModel(messageType, startTime, duration, dropAtDelivery));
                break;
            default:
                throw new ParseException("SpecificMessageFault:  network type " + context + " not recognized", 0);
        }
        setName("SpecificMessageFault-0x" + Integer.toHexString(messageType));
        
    }
    
    //parse a string into a CAN base ID + replication and return it.
    
    public static int getNetworkMessageID(String[] args, int startIndex) throws ParseException {
        String idStr = args[startIndex];

        String messageName;
        int replicationOffset = 0;
        int messageID = 0;
        if (idStr.contains("+")) {
            //replicated
            String[] split = idStr.split("\\+");
            if (split.length != 2) {
                throw new ParseException("Too many + signs in input", 0);
            }
            messageName = split[0] + "_BASE_CAN_ID";
            replicationOffset = computeReplicationID(split[1]);
        } else {
            //non-replicated
            messageName = idStr + "_CAN_ID";
            replicationOffset = 0;
        }
        try {
            messageID = (Integer) rf.getStaticFieldValue(MessageDictionary.class, messageName);
        } catch (Exception ex) {
            throw new ParseException("No message id named " + messageName + " found in Message Dictionary:  " + ex.getMessage(), 0);
        }
        return messageID + replicationOffset;
    }

    public static int computeReplicationID(String replicationString) throws ParseException {
        if (!replicationString.startsWith("rep(") ||
                !replicationString.endsWith(")")) {
            throw new ParseException("Replication string does not have the correct form: \"rep(...)\"", 0);
        }
        String[] repValues = replicationString.substring(4, replicationString.length() - 1).split(",");
        try {
            Method m = rf.getMethod(ReplicationComputer.class, "computeReplicationID", Arrays.asList(repValues));
            return (Integer) rf.invoke(m, null, Arrays.asList(repValues));
        } catch (Exception ex) {
            throw new ParseException("Failed to find a methods in ReplicationComputer that matched the signature " + replicationString + ":  " + ex.getMessage(), 0);
        }
    }

    
    /**
     * Creates a Payload object from the argument string and return the payload ID.
     */
    private static int getFrameworkMessageID(String[] args, int startIndex) throws ParseException {
        int index = startIndex;
        if (args.length < 4) {
            throw new ParseException("FrameworkMessageFaultModel expects at least two arguments, not " + args.length, 0);
        }

        ReflectionFactory refFactory = new ReflectionFactory(false);
        List<String> constructorArgs = new ArrayList<String>();
        List<String> packagePath = new ArrayList<String>();
        Payload payload = null;
        String payloadName;

        payloadName = args[index] + "Payload";
        index++;
        packagePath.add("simulator.payloads.");

        //put the remaining arbuments into the constructor
        for (; index < args.length; index++) {
            constructorArgs.add(args[index]);
        }
        
        String[] strings = constructorArgs.toArray(new String[constructorArgs.size()]);

        try {
            //create payload for framework payload object
            payload = (Payload)refFactory.createObjectFromStrings(payloadName, packagePath, strings);
        } catch (Exception e) {
            throw new ParseException("could not call constructor for " + payloadName + " with arguments " + refFactory.commaConcatenate(strings) + ":  " + e, -1);
        }
        return payload.getType();
    }
}
