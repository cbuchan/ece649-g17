/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework;

import jSimPack.SimTime.SimTimeUnit;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import simulator.elevatorcontrol.MessageDictionary;
import simulator.elevatormodules.Modules;

/**
 * Utility class for instantiating controllers from the .cf file for unit and
 * integration tests.
 *
 * @author justinr2
 */
public class ControllerBuilder {

    private ReflectionFactory rf = new ReflectionFactory(false);
    private List<String> packagePath = new ArrayList<String>();

    public ControllerBuilder() {
        packagePath.add("simulator.elevatorcontrol.");
    }

    private class ControlFileParser implements Parser {

        private final boolean verbose;
        private final ControllerSet controllers = new ControllerSet();

        public ControlFileParser(boolean verbose) {
            this.verbose = verbose;
        }

        public void parse(String[] args, FileTokenizer sourceFT) throws ParseException {
            if (verbose) {
                Harness.log("Control", "parsing ", Arrays.toString(args));
            }


            String controllerName = args[0];
            String[] controllerArgs = new String[args.length-1];
            for (int i=0; i < controllerArgs.length; i++) {
                controllerArgs[i] = args[i+1];
            }

            Object controller = null;
            try {
                controller = rf.createObjectFromStrings(controllerName, packagePath, controllerArgs);
            } catch (Exception ex) {
                throw new ParseException(sourceFT.lineMessage("Error constructing control object: " + ex + "\n String arguments: " + Arrays.toString(args)), -1);
            }
            if (!(controller instanceof Controller)) {
                throw new ParseException(sourceFT.lineMessage("Unexpectedly created an object that was not a controller object."), -1);
            }
            controllers.add((Controller) controller);
        }

        public ControllerSet getControllerSet() {
            return controllers;
        }
    }

    public static void printDefines() {

        //first, print all network periods
        printComment("The following lines are automatically generated and designed to be compatible with the .mf #DEFINE directive.");
        printComment("controller periods");
        printDefine("HALL_BUTTON_CONTROL_PERIOD", MessageDictionary.HALL_BUTTON_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("CAR_BUTTON_CONTROL_PERIOD", MessageDictionary.CAR_BUTTON_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("LANTERN_CONTROL_PERIOD", MessageDictionary.LANTERN_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("CAR_POSITION_CONTROL_PERIOD", MessageDictionary.CAR_POSITION_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DISPATCHER_PERIOD", MessageDictionary.DISPATCHER_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DOOR_CONTROL_PERIOD", MessageDictionary.DOOR_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DRIVE_CONTROL_PERIOD", MessageDictionary.DRIVE_CONTROL_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printBlank();
        printComment("module periods");
        printDefine("AT_FLOOR_PERIOD", Modules.AT_FLOOR_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("WEIGHT_PERIOD", Modules.WEIGHT_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("CAR_LEVEL_POSITION_PERIOD", Modules.CAR_LEVEL_POSITION_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DOOR_OPENED_SENSOR_PERIOD", Modules.DOOR_OPENED_SENSOR_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DOOR_CLOSED_SENSOR_PERIOD", Modules.DOOR_CLOSED_SENSOR_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DOOR_REVERSAL_NETWORK_PERIOD", Modules.DOOR_REVERSAL_NETWORK_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("DRIVE_PERIOD", Modules.DRIVE_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("HOISTWAY_LIMIT_PERIOD", Modules.HOISTWAY_LIMIT_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printDefine("LEVEL_SENSOR_PERIOD", Modules.LEVEL_SENSOR_PERIOD.toString(SimTimeUnit.MILLISECOND));
        printBlank();
        printComment("Controller CAN Ids");
        printComment("NOTE:  These IDs assume you use the ReplicationComputer offsets and MessageDictionary base values");

        //single controllers
        printDefine("DRIVE_SPEED_CAN_ID", MessageDictionary.DRIVE_SPEED_CAN_ID);
        printDefine("DRIVE_COMMAND_CAN_ID", MessageDictionary.DRIVE_COMMAND_CAN_ID);
        for (Hallway h : Hallway.replicationValues) {
            printDefine("DESIRED_DWELL_" + ReplicationComputer.makeReplicationString(h) + "_CAN_ID",
                    MessageDictionary.DESIRED_DWELL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(h));
        }
        printDefine("DESIRED_FLOOR_CAN_ID", MessageDictionary.DESIRED_FLOOR_CAN_ID);
        printDefine("CAR_POSITION_CAN_ID", MessageDictionary.CAR_POSITION_CAN_ID);
        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                printDefine("DOOR_MOTOR_COMMAND_" + ReplicationComputer.makeReplicationString(h, s) + "_CAN_ID",
                        MessageDictionary.DOOR_MOTOR_COMMAND_BASE_CAN_ID + ReplicationComputer.computeReplicationId(h, s));
            }
        }
        for (Direction d : Direction.replicationValues) { 
			
		}
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                if (!Elevator.hasLanding(floor, h)) {
                    continue;
                }
                for (Direction d : Direction.replicationValues) {
                    if (floor == 8 && d == Direction.UP) {
                        continue;
                    }
                    if (floor == 1 && d == Direction.DOWN) {
                        continue;
                    }
                    printDefine("HALL_CALL_" + ReplicationComputer.makeReplicationString(floor, h, d) + "_CAN_ID",
                            MessageDictionary.HALL_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, h, d));
                    printDefine("HALL_LIGHT_" + ReplicationComputer.makeReplicationString(floor, h, d) + "_CAN_ID",
                            MessageDictionary.HALL_LIGHT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, h, d));
                }
            }
        }
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                if (!Elevator.hasLanding(floor, h)) {
                    continue;
                }
                printDefine("CAR_CALL_" + ReplicationComputer.makeReplicationString(floor, h) + "_CAN_ID",
                        MessageDictionary.CAR_CALL_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, h));
                printDefine("CAR_LIGHT_" + ReplicationComputer.makeReplicationString(floor, h) + "_CAN_ID",
                        MessageDictionary.CAR_LIGHT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, h));
            }
        }
        for (Direction d : Direction.replicationValues) {
            printDefine("CAR_LANTERN_" + ReplicationComputer.makeReplicationString(d) + "_CAN_ID",
                    MessageDictionary.CAR_LANTERN_BASE_CAN_ID + ReplicationComputer.computeReplicationId(d));
		}


        printBlank();
        printComment("Module CAN Ids");
        printComment("NOTE:  These IDs assume you use the ReplicationComputer offsets and MessageDictionary base values");

        for (Direction d : Direction.replicationValues) {
            printDefine("LEVEL_SENSOR_" + ReplicationComputer.makeReplicationString(d) + "_CAN_ID",
                    MessageDictionary.LEVELING_BASE_CAN_ID + ReplicationComputer.computeReplicationId(d));
		}
        for (int floor = 1; floor <= Elevator.numFloors; floor++) {
            for (Hallway h : Hallway.replicationValues) {
                if (!Elevator.hasLanding(floor, h)) {
                    continue;
                }
                printDefine("AT_FLOOR_" + ReplicationComputer.makeReplicationString(floor, h) + "_CAN_ID",
                        MessageDictionary.AT_FLOOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(floor, h));

            }
        }
        printDefine("CAR_LEVEL_POSITION_CAN_ID", MessageDictionary.CAR_LEVEL_POSITION_CAN_ID);
        printDefine("CAR_WEIGHT_CAN_ID", MessageDictionary.CAR_WEIGHT_CAN_ID);
        printDefine("CAR_WEIGHT_ALARM_CAN_ID", MessageDictionary.CAR_WEIGHT_ALARM_CAN_ID);
        for (Hallway h : Hallway.replicationValues) {
            for (Side s : Side.values()) {
                printDefine("DOOR_CLOSED_SENSOR_" + ReplicationComputer.makeReplicationString(h, s) + "_CAN_ID",
                        MessageDictionary.DOOR_CLOSED_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(h, s));
                printDefine("DOOR_OPEN_SENSOR_" + ReplicationComputer.makeReplicationString(h, s) + "_CAN_ID",
                        MessageDictionary.DOOR_OPEN_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(h, s));
                printDefine("DOOR_REVERSAL_SENSOR_" + ReplicationComputer.makeReplicationString(h, s) + "_CAN_ID",
                        MessageDictionary.DOOR_REVERSAL_SENSOR_BASE_CAN_ID + ReplicationComputer.computeReplicationId(h, s));
            }
        }
        for (Direction d : Direction.replicationValues) {
            printDefine("HOISTWAY_LIMIT_" + ReplicationComputer.makeReplicationString(d) + "_CAN_ID",
                    MessageDictionary.HOISTWAY_LIMIT_BASE_CAN_ID + ReplicationComputer.computeReplicationId(d));
        }
        printDefine("EMERGENCY_BRAKE_CAN_ID", MessageDictionary.EMERGENCY_BRAKE_CAN_ID);
    }
    /**
     * Print a define statement compatible with the message injector formats
     * @param name
     * @param value
     */
    private final static int NAME_COL_WIDTH = 45;

    private static void printDefine(String name, String value) {
        System.out.print("#DEFINE " + name);
        for (int i = name.length(); i < NAME_COL_WIDTH; i++) {
            System.out.print(" ");
        }
        System.out.println(" " + value);
    }

    /**
     * Convenience overload for printing integer values in hex format.
     * @param name
     * @param value
     */
    private static void printDefine(String name, int value) {
        printDefine(name, " 0x" + Integer.toHexString(value).toUpperCase());
    }

    private static void printComment(String message) {
        System.out.println(";" + message);
    }

    private static void printBlank() {
        System.out.println();
    }

    /**
     * Instantiates all controllers in the elevator
     *
     * Descriptions of the objects are read from a file.  Each line of
     * the file is expected to contain all of the arguments expected by the
     * Control object's constructor, in the order the Constructor expects them.
     * The first word on each line must be the name of the class of the
     * Controller.  So, one could instantiate a DriveControl object by including
     * a line like this:
     *
     * <pre>
     *      DriveControl 8 100000 true
     * </pre>
     *
     * The effect will be the same as calling the constructor:
     *
     * <pre>
     *      DriveControl(8,100000,true);
     * </pre>
     *
     * This method uses Java reflection, so you need Java 1.5 or later to use it.
     */
    public ControllerSet makeFromFile(String filename, boolean verbose) {
        ControlFileParser control = new ControlFileParser(verbose);

        FileTokenizer fp = new FileTokenizer(filename, verbose, control);

        fp.parseFile();

        return control.getControllerSet();
    }

    /**
     * Creates all the objects in the elevator.  This method is used for acceptance tests.
     *
     * Objects are created through reflection (rather than being instantiated directly)
     * so that the simulator will compile even if all modules are not present.
     *
     * This allows you to perform unit and integration tests even if the
     * elevator is not complete.
     *
     * The controllers you implement in simulator.elevatorcontrol MUST have a controller
     * that matches the signatures used here.  The objects passed to the contructor
     * are all the objects that follow "packagePath" in the createControllerObject() call.
     *
     * @return a controllerSet containing all the controllers instantiated for the acceptance test.
     * @param verbose
     */
    public ControllerSet makeAll(boolean verbose) {

        ControllerSet controllers = new ControllerSet();

        try {
            for (int floor = 1; floor <= Elevator.numFloors; ++floor) {
                for (Hallway hallway : Hallway.replicationValues) {
                    if (Elevator.hasLanding(floor, hallway)) {
                        controllers.add(createControllerObject("CarButtonControl", floor, hallway, MessageDictionary.CAR_BUTTON_CONTROL_PERIOD, verbose));
                        //controllers.add(new CarButtonControl(floor, hallway, MessageDictionary.CAR_BUTTON_CONTROL_PERIOD, verbose));
                        if (floor != 1) {
                            controllers.add(createControllerObject("HallButtonControl", floor, hallway, Direction.DOWN, MessageDictionary.HALL_BUTTON_CONTROL_PERIOD, verbose));
                            //controllers.add(new HallButtonControl(floor, hallway, Direction.DOWN, MessageDictionary.HALL_BUTTON_CONTROL_PERIOD, verbose));
                        }
                        if (floor != 8) {
                            controllers.add(createControllerObject("HallButtonControl", floor, hallway, Direction.UP, MessageDictionary.HALL_BUTTON_CONTROL_PERIOD, verbose));
                            //controllers.add(new HallButtonControl(floor, hallway, Direction.UP, MessageDictionary.HALL_BUTTON_CONTROL_PERIOD, verbose));
                        }
                    }
                }
            }

            controllers.add(createControllerObject("Dispatcher", Elevator.numFloors, MessageDictionary.DISPATCHER_PERIOD, verbose));
            //controllers.add(new Dispatcher(Elevator.numFloors, MessageDictionary.DISPATCHER_PERIOD, verbose));

            for (Hallway h : Hallway.replicationValues) {
                for (Side s : Side.values()) {
                    controllers.add(createControllerObject("DoorControl", h, s, MessageDictionary.DOOR_CONTROL_PERIOD, verbose));
                    //controllers.add(new DoorControl(h, s, MessageDictionary.DOOR_CONTROL_PERIOD, verbose));
                }
            }

            controllers.add(createControllerObject("DriveControl", MessageDictionary.DRIVE_CONTROL_PERIOD, verbose));
            //controllers.add(new DriveControl(MessageDictionary.DRIVE_CONTROL_PERIOD, verbose));

            controllers.add(createControllerObject("CarPositionControl", MessageDictionary.CAR_POSITION_CONTROL_PERIOD, verbose));
            //controllers.add(new CarPositionControl(MessageDictionary.CAR_POSITION_CONTROL_PERIOD, verbose));

            for (Direction d : Direction.replicationValues) {
                controllers.add(createControllerObject("LanternControl", d, MessageDictionary.LANTERN_CONTROL_PERIOD, verbose));
                //controllers.add(new LanternControl(d, MessageDictionary.LANTERN_CONTROL_PERIOD, verbose));
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Exception while building controller set for acceptance testing.  This probably means one or more controllers is missing from simulator.elevatorcontrol.", ex);
        } catch (IllegalArgumentException ex) {
            if (ex.getCause() instanceof InvocationTargetException) {
                throw new RuntimeException("Exception while building controller set for acceptance testing.  This probably means an exception occured in the class constructor.", ex);
            } else {
                throw new RuntimeException("Exception while building controller set for acceptance testing.  This probably means that the constructor signature does not match simulator.framework.ControllerBuilder.makeAll().  See the method documentation for details.\n Specific error: ", ex);
            }
//        } catch (InvocationTargetException ex) {
//            throw new RuntimeException("Exception while building controller set for acceptance testing.  This probably means an exception occured in the class constructor.", ex);
        } catch (Throwable ex) {
            throw new RuntimeException("Exception while building controller set for acceptance testing.  No more specific information is available.", ex);
        }

        return controllers;
    }

    private Controller createControllerObject(String name, Object... params) throws ClassNotFoundException {
        return (Controller) rf.createObjectFromParameters(name, packagePath, params);
    }
}
