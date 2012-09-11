package simulator.framework;

import jSimPack.BreakpointPrinter;
import jSimPack.SimTime;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import simulator.elevatormodules.*;
import simulator.elevatormodules.passengers.PassengerControl;
import simulator.elevatormodules.passengers.PassengerHandler;

/**
 * Provides command-line interface to and instantiates all objects for the
 * Elevator simulation.
 * 
 * @author Christopher Martin
 * @author Charles Shelton
 * @author Nick Zamora
 * @author Beth Latronico
 * @author Kenny Stauffer
 * @author Justin Ray
 */
public class Elevator {

    public static final int TIMER_VERBOSITY = 1;
    public static final int MaxCarCapacity = 14000;
    public static final SimTime ACCEPTANCE_DEFAULT_RUNTIME = new SimTime(1, SimTime.SimTimeUnit.HOUR);
    public static final SimTime TEST_DEFAULT_RUNTIME = new SimTime(5, SimTime.SimTimeUnit.SECOND);
    public static final double DISTANCE_BETWEEN_FLOORS = 5.0;
    /**
     * Maximum car capacity in tenths of pounds.
     */
    private static String headerText = "";
    private static SimTime canNetworkBitWidth = SimTime.ZERO;
    private static boolean[][] landings = {
        //front, back
        {true, true}, //first floor
        {false, true}, //second floor has back landing only
        {true, false},
        {true, false},
        {true, false},
        {true, false},
        {true, true},
        {true, false}, //eighth floor
    };
    public static final int numFloors = landings.length;
    private static final SimTime PROGRESS_INTERVAL = new SimTime(1, SimTime.SimTimeUnit.MINUTE);
    private static double fastElevatorSpeed = 1.0; //m/s
    private static boolean ignoreLeveling = false; 

    /**
     * 
     * @return the "FAST" elevator speed, as set by the -fs commandline option
     */
    public static double getFastElevatorSpeed() {
        return fastElevatorSpeed;
    }
    
    /**
     * 
     * @return True if the passengers should ignore the board/entering leveling requirement, false otherwise
     */
    public static boolean getIgnoreLeveling() {
    	return ignoreLeveling;
    }

    /**
     * 
     * @param floor 1-indexed floor number
     * @param hallway hallway
     * @return True if the elevator has a landing at that floor and hallway
     */
    public static boolean hasLanding(int floor, Hallway hallway) {
        if (floor < 1 || floor > numFloors) {
            throw new IndexOutOfBoundsException("no such floor: " + floor);
        }
        return landings[floor - 1][hallway.ordinal()];
    }

    /**
     * parse commandline arguments and instantiate the appropriate objects for simulation
     * @param Args
     */
    public static void main(String[] Args) {

        long startTime = System.currentTimeMillis();

        String peopleFile = null;
        String faultFile = null;
        String messageFile = null;
        String controllerFile = null;
        String realtimeRate = null;
        String breakpointString = null;
        ArrayList<String> monitorNames = new ArrayList<String>();

        /* set the defaults */

        SimTime simRunTime = null;

        boolean controllerVerbose = false;
        boolean faultInjectorVerbose = false;
        boolean frameworkVerbose = false;
        boolean messageInjectorVerbose = false;
        boolean networkVerbose = false;
        boolean frameworkNetworkVerbose = false;
        boolean peopleVerbose = false;
        boolean utilizationVerbose = false;
        boolean dropVerbose = false;


        boolean showDisplay = false;

        if (Args.length == 0) {
            printFullUsage();
            return;
        }

        // parse Args

        for (int paramNum = 0; paramNum < Args.length; ++paramNum) {
            try {
                if (Args[paramNum].equals("-cf")) {
                    paramNum++;
                    controllerFile = Args[paramNum];
                } else if (Args[paramNum].equals("-mf")) {
                    paramNum++;
                    messageFile = Args[paramNum];
                } else if (Args[paramNum].equals("-pf")) {
                    paramNum++;
                    peopleFile = Args[paramNum];
                } else if (Args[paramNum].equals("-pd")) {
                    paramNum++;
                    //short circuit the rest of the simulator and just print the defines
                    ControllerBuilder.printDefines();
                    System.exit(0);
                } else if (Args[paramNum].equals("-ff")) {
                    paramNum++;
                    faultFile = Args[paramNum];
                } else if (Args[paramNum].equals("-gui")) {
                    showDisplay = true;
                } else if (Args[paramNum].equals("-rt")) {
                    paramNum++;
                    simRunTime = new SimTime(Args[paramNum]);
                } else if (Args[paramNum].equals("-b")) {
                    paramNum++;
                    //bitsPerSecond = Double.parseDouble(Args[paramNum])*1000;

                    Double bitsPerSecond = Double.parseDouble(Args[paramNum]) * 1000;
                    //compute bit timing
                    canNetworkBitWidth = new SimTime(1 / bitsPerSecond, SimTime.SimTimeUnit.SECOND);
                } else if (Args[paramNum].equals("-uv")) {
                    paramNum++;
                    utilizationVerbose = true;
                } else if (Args[paramNum].equals("-rate")) {
                    paramNum++;
                    realtimeRate = Args[paramNum];
                } else if (Args[paramNum].equals("-seed")) {
                    paramNum++;
                    Harness.setRandomSeed(Long.parseLong(Args[paramNum]));
                } else if (Args[paramNum].equals("-fs")) {
                    paramNum++;
                    fastElevatorSpeed = Double.parseDouble(Args[paramNum]);
                    if (fastElevatorSpeed < 1.0 || fastElevatorSpeed > 10.0) {
                        System.err.println("-fs value must be in the range [1.0, 10.0].");
                        printSmallUsage();
                        return;
                    }
                } else if (Args[paramNum].equals("-head")) {
                    paramNum++;
                    File headerfile = new File(Args[paramNum]);
                    if (!headerfile.exists()) {
                        throw new IOException("Header file '" + Args[paramNum] + "' does not exist.");
                    }
                    try {
                        BufferedReader headerIn = new BufferedReader(new FileReader(headerfile));
                        StringBuilder buf = new StringBuilder();
                        while (headerIn.ready()) {
                            buf.append(headerIn.readLine());
                            buf.append("\n");
                        }
                        headerIn.close();
                        headerText = buf.toString();
                    } catch (Exception ex) {
                        throw new IOException("There was a problem reading the header file: " + ex.getMessage());
                    }
                } else if (Args[paramNum].equals("-monitor")) {
                    paramNum++;
                    monitorNames.add(Args[paramNum]);
                } else if (Args[paramNum].equals("-break")) {
                    paramNum++;
                    breakpointString = Args[paramNum];
                } else if (Args[paramNum].equals("-lf")) {
                    paramNum++;
                    Harness.setLogFilename(Args[paramNum]);
                } else if (Args[paramNum].equals("-cv")) {
                    controllerVerbose = true;
                } else if (Args[paramNum].equals("-nv")) {
                    networkVerbose = true;
                } else if (Args[paramNum].equals("-fnv")) {
                    frameworkNetworkVerbose = true;
                } else if (Args[paramNum].equals("-fv")) {
                    frameworkVerbose = true;
                } else if (Args[paramNum].equals("-pv")) {
                    peopleVerbose = true;
                } else if (Args[paramNum].equals("-miv")) {
                    messageInjectorVerbose = true;
                } else if (Args[paramNum].equals("-il")) {
					ignoreLeveling = true;
                } else if (Args[paramNum].equals("-fiv")) {
                    faultInjectorVerbose = true;
                } else if (Args[paramNum].equals("-dropv")) {
                    dropVerbose = true;
                } else {
                    System.err.println("Unrecognized argument: "
                            + Args[paramNum]);
                    printSmallUsage();
                    return;
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Error while parsing argument: " + Args[paramNum]);
                System.err.println(nfe.getMessage());
                printSmallUsage();
                return;
            } catch (IOException ex) {
                System.err.println("Error while parsing argument: " + Args[paramNum]);
                System.err.println(ex.getMessage());
                printSmallUsage();
                return;
            }
        }

        //set the realtimeRate
        if (realtimeRate == null) {
            if (showDisplay) {
                Harness.setRealtimeRate(1.0);
            } else {
                Harness.setRealtimeRate(Double.POSITIVE_INFINITY);
            }
        } else if (realtimeRate.toLowerCase().startsWith("inf")) {
            Harness.setRealtimeRate(Double.POSITIVE_INFINITY);
        } else {
            try {
                double rate = Double.parseDouble(realtimeRate);
                if (Double.compare(rate, 0.0) == 0 && !showDisplay) {
                    System.err.println("Rate of 0 is valid only if the GUI is enabled.");
                    printSmallUsage();
                    return;
                }
                if (Double.compare(rate, 0.0) < 0) {
                    System.err.println("Rate must be non-negative: " + realtimeRate);
                    printSmallUsage();
                    return;
                }
                Harness.setRealtimeRate(rate);
            } catch (NumberFormatException nfe) {
                System.err.println("Error while parsing rate argument: " + realtimeRate);
                System.err.println(nfe.getMessage());
                printSmallUsage();
                return;
            }
        }

        //set breakpoints
        if (breakpointString != null) {
            if (showDisplay) {
                Harness.addBreakpointListener(new BreakpointPrinter(System.err));
                String[] breakpoints = breakpointString.split(",", 0);
                try {
                    //okay to set breakpoints because the GUI can restart the simulation
                    for (String b : breakpoints) {
                        Harness.addBreakpoint(new SimTime(b));
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println("Error while parsing breakpoints: " + nfe.getMessage());
                    printSmallUsage();
                    return;
                }
            } else {
                System.err.println("Breakpoints can be used only if the GUI is enabled.");
                return;
            }
        }


        //always create a new progress logger
        new ProgressLogger("Elevator", PROGRESS_INTERVAL);

        Harness.initialize(canNetworkBitWidth, networkVerbose, frameworkNetworkVerbose, utilizationVerbose, dropVerbose);

        // The only valid invocations are (message file and controller file),
        // or (people file, possibly with a fault file).  Any other
        // combination is invalid and should exit immediately.

        Timer.setVerbosity(TIMER_VERBOSITY);

        if (headerText.length() > 0) {
            System.out.println(headerText);
            System.out.println();
        }

        //make a stringwriter to take all the parameter info
        //so we can print it to the summary file too
        StringWriter parameterStringWriter = new StringWriter();
        PrintWriter paramWriter = new PrintWriter(parameterStringWriter);

        paramWriter.println("Simulator Parameters:");
        paramWriter.print("Command line =");
        {
            StringBuilder cmdLine = new StringBuilder();
            for (String arg : Args) {
                cmdLine.append(" ");
                cmdLine.append(arg);
            }
            paramWriter.println(cmdLine.toString());
        }
        paramWriter.println("RandomSeed = " + Harness.getRandomSeed());
        paramWriter.println("CanNetworkBitTime = " + canNetworkBitWidth.getTruncNanoseconds() + " ns");
        paramWriter.println("Effective Can Bitrate = " + (1 / canNetworkBitWidth.getFracSeconds()));
        paramWriter.println("Run Time = " + simRunTime);
        paramWriter.println("Real time rate = " + realtimeRate);
        if (breakpointString != null) {
            paramWriter.println("Breakpoints = " + breakpointString);
        }

        if (messageFile != null && controllerFile != null
                && peopleFile == null && faultFile == null) {

            if (!monitorNames.isEmpty()) {
                System.err.println("You can only use monitors on acceptance tests");
                printSmallUsage();
                System.exit(1);
            }

            if (simRunTime == null) {
                //set default for testing
                simRunTime = TEST_DEFAULT_RUNTIME;
            }

            //print the last bit of the header
            paramWriter.println("Message File = " + messageFile);
            paramWriter.println("Controller File = " + controllerFile);
            paramWriter.println();
            //print the parameter to screen
            System.out.println(parameterStringWriter.toString());

            // unit test or integration test
            ControllerBuilder cb = new ControllerBuilder();
            ControllerSet theControllers = cb.makeFromFile(controllerFile, controllerVerbose);

            MessageInjector theMI = new MessageInjector(messageFile,
                    theControllers,
                    messageInjectorVerbose);


            if (showDisplay) {
                System.err.println("The GUI can be used only for acceptance tests.");
                printSmallUsage();
                System.exit(-1);
            }

            Harness.log("Elevator", "Starting Simulation ***");
            //Harness.log("Elevatar","RandomSeed = " + Harness.getRandomSeed());
            Harness.runSim(SimTime.add(theMI.lastInjectionTime(), simRunTime));
            Harness.log("Elevator", "Simulation Finished ***");

            System.out.println(theMI.getAssertionSummary());

            //write stats to a file
            int counter = 0;
            File statsFile;
            do {
                //create a file object and get the file name from it 
                //so that the filename is not broken by relative paths
                //in the peopleFile string object.
                statsFile = new File(String.format("injection-%s-%d.stats", new File(messageFile).getName(), counter));
                counter++;
            } while (statsFile.exists());
            try {
                FileWriter fw = new FileWriter(statsFile);
                fw.write(headerText + "\n");
                fw.write(parameterStringWriter.toString());
                fw.write(theMI.getAssertionStats() + "\n");
                fw.flush();
                fw.close();
            } catch (IOException ex) {
                System.out.println("Could not write injection stats to file \"" + statsFile + "\":  " + ex.getMessage());
            }



        } else if (peopleFile != null && controllerFile == null
                && messageFile == null) {

            // acceptance testing mode


            if (simRunTime == null) {
                //set default for acceptance tests
                simRunTime = ACCEPTANCE_DEFAULT_RUNTIME;
            }

            //print the last bit of the header
            paramWriter.println("Acceptance Test File = " + peopleFile);
            if (faultFile != null) {
                paramWriter.println("Fault Injection File = " + faultFile);
            }
            if (ignoreLeveling == true) {
            	paramWriter.println("Passengers ignoring leveling requirement");
            }

            ArrayList<RuntimeMonitor> monitors = new ArrayList<RuntimeMonitor>();

            if (!monitorNames.isEmpty()) {
                paramWriter.print("Monitors: ");
                for (String name : monitorNames) {
                    //write the name
                    paramWriter.print(name + "  ");
                    //create the monitor
                    RuntimeMonitor mon = RuntimeMonitor.createMonitor(name);
                    monitors.add(mon);
                }
                paramWriter.println();
            }
            paramWriter.println();
            //print the parameter to screen
            System.out.println(parameterStringWriter.toString());

            // MessageDictionary.makeFromFile(controllerFile, controllerVerbose);
            ControllerBuilder cb = new ControllerBuilder();
            cb.makeAll(controllerVerbose);

            //create modules, keep a reference to the passenger control
            //to pass to passenger objects
            PassengerControl pc = Modules.makeAll(frameworkVerbose);

            //create a parser to read the passenger file
            PassengerParser pi =
                    new PassengerParser(peopleFile, Harness.getRandomSeed(), pc, peopleVerbose);

            //passenger handler does all the heavy lifting with passengers
            final PassengerHandler ph = new PassengerHandler(pc, pi.getPassengers(), peopleVerbose);

            if (showDisplay) {
                try {
                    java.awt.EventQueue.invokeAndWait(new Runnable() {

                        public void run() {
                            new SwingDisplay(ph, true).setVisible(true);
                        }
                    });
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                    System.exit(-1);
                }
            }


            FaultInjector faultInj = null;
            if (null != faultFile) {
                faultInj = new FaultInjector(faultFile, faultInjectorVerbose);
            }

            try {
                Harness.log("Elevator", "Starting Simulation");
                Harness.runSim(SimTime.add(simRunTime, pi.getLastInjectionTime()));
                Harness.log("Elevator", "Simulation Finished");
                System.out.println();

                //network utilization
                if (Harness.getCANNetwork().getUtilization().isEnabled()) {
                    System.out.println("Network Utilization Summary:");
                    System.out.println(Harness.getCANNetwork().getUtilization());
                    System.out.println();
                }

                if (faultInj != null) {
                    System.out.println(faultInj.getStats());
                    System.out.println();
                }

                if (!monitors.isEmpty()) {
                    System.out.println("Monitors Warning Results: ");
                    for (RuntimeMonitor mon : monitors) {
                        System.out.println(mon.getWarningStats());
                    }
                    System.out.println();

                    System.out.println("Monitors Summmary Results: ");
                    for (RuntimeMonitor mon : monitors) {
                        String[] strarr = mon.summarize();
                        for (String s : strarr) {
                            System.out.println(s);
                        }
                    }
                    System.out.println();
                }

                //write passenger stats to screen
                System.out.println(ph.getSummaryStats());
                System.out.println();

                //write stats to a file
                int counter = 0;
                File statsFile;
                do {
                    //create a file object and get the file name from it 
                    //so that the filename is not broken by relative paths
                    //in the peopleFile string object.
                    File peopleFileObj = new File(peopleFile);
                    statsFile = new File(String.format("elevator-%s-%d.stats", peopleFileObj.getName(), counter));
                    counter++;
                } while (statsFile.exists());
                try {
                    FileWriter fw = new FileWriter(statsFile);
                    fw.write(headerText + "\n");
                    fw.write(parameterStringWriter.toString());
                    fw.write(ph.getStats() + "\n");
                    fw.write("\n");
                    fw.write("\n");
                    if (faultInj != null) {
                        fw.write(faultInj.getStats() + "\n");
                        fw.write("\n");
                    }
                    if (Harness.getCANNetwork().getUtilization().isEnabled()) {
                        fw.write("Network Utilization Summary:\n");
                        fw.write(Harness.getCANNetwork().getUtilization() + "\n");
                        fw.write("\n");
                    }
                    if (!monitors.isEmpty()) {
                        fw.write("Monitors Warning Results: \n");
                        for (RuntimeMonitor mon : monitors) {
                            fw.write(mon.getWarningStats() + "\n");
                        }
                        fw.write("\n");

                        fw.write("Monitors Summmary Results: \n");
                        for (RuntimeMonitor mon : monitors) {
                            String[] strarr = mon.summarize();
                            for (String s : strarr) {
                                fw.write(s + "\n");
                            }
                        }
                        fw.write("\n");
                    }
                    fw.flush();
                    fw.close();
                } catch (IOException ex) {
                    System.out.println("Could not write passenger stats to file \"" + statsFile + "\":  " + ex.getMessage());
                }


            } catch (SafetyViolationException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace(System.out);
                //exit with error
                System.exit(-1);
            }
            double runtime = (double)(System.currentTimeMillis() - startTime) / 1000;
            SimTime endTime = Harness.getTime();
            System.out.println(String.format("%s simulation seconds\n %.3f real seconds\n effective rate %.2f",endTime.toString(), runtime, endTime.getFracSeconds() / runtime));

        } else {
            printSmallUsage();
        }
        // this will close the GUI if it's open
        System.exit(0);
    }

    /**
     * Short usage for when there are commandline errors
     */
    public static void printSmallUsage() {
        System.out.println(
                "\n"
                + "SYNOPSIS\n"
                + "  Elevator [options] {-cf <file> -mf <file> | -pf <file> [-ff <file>]}\n"
                + "\n"
                + "DESCRIPTION\n"
                + "  Either a unit/integration test (-cf with mandatory -mf) or an acceptance test\n"
                + "  (-pf with optional -ff) must be specified.\n"
                + "\n"
                + "  Run \"Elevator\" with no arguments for detailed instructions.\n");
    }

    static void log(Object... msg) {
        Harness.log("Elevator", msg);
    }

    /**
     * Complete command-line reference.  It is important that this be kept in sync 
     * with the commandline options parser and the message, configuration, fault,
     * and passenger file parsers.  This is the primary reference for using the simulator.
     */
    public static void printFullUsage() {
        System.out.println(
"SYNOPSIS\n"
+ "  Elevator [options] {-cf <file> -mf <file> | -pf <file> [-ff <file>]}\n"
+ "\n"
+ "DESCRIPTION\n"
+ "  Either a unit/integration test (-cf with mandatory -mf) or an acceptance\n"
+ "  test (-pf with optional -ff) must be specified.\n"
+ "\n"
+ "OUTPUT\n"
+ "  A file with the \"stats\" extension will be placed in the working directory.\n"
+ "  where the simulator is executed.  The name of the file will be related to the\n"
+ "  unit, integration, or acceptance test files used.  Files include an incrementing\n"
+ "  index value so that previous results will not be overwritten.\n"
+ "  NOTE:  The stats file is printed at the end of the simulation, so if the\n"
+ "  elevator terminates on an exception, a stats file is usually not printed, or\n"
+ "  may be incomplete.\n"
+ "  \n"
+ "  Acceptance test results include detailed passenger delivery information and\n"
+ "  detailed information on passenger satisfaction scores.\n"
+ "  \n"
+ "  Unit and integration test results include information on passed and failed\n"
+ "  assertions\n"
+ "  \n"
+ "  In addition to the stats file, some information will be printed to stdout,\n"
+ "  including a progress message every 60 seconds and a summary of the test \n"
+ "  results.\n"
+ "\n"
+ "TIME REPRESENTATIONS\n"
+ "  Any part of the simulator that requires a time specification expects input\n"
+ "  in the following format.  This includes any time columns in any of the input\n"
+ "  files describe below.\n"
+ "  \n"
+ "  The time representation is a postive integer or floating point value followed\n"
+ "  by one of the units specified below.  For example, \"1.5s\" will parse as 1.5\n"
+ "  seconds or 1500 ms.  Note that the simulator will round to the minimum\n"
+ "  resolution (1ns) so if you specify a time smaller than that (e.g.  \"0.3ns\",\n"
+ "  this value will be rounded to zero.\n"
+ "\n"
+ "       h - hour\n"
+ "       m - minute\n"
+ "       s - second\n"
+ "       ms - millisecond\n"
+ "       us - microsecond\n"
+ "       ns - nanosecond\n"
+ "\n"
+ "  Depending on the context, time values may represent a duration or they may\n"
+ "  represent a moment in time in the simulation (an offset from the simulation\n"
+ "  start time).\n"
+ "\n"
+ "OPTIONS\n"
+ "  -pd\n"
+ "    Print an exhaustive set of #DEFINE statements for all controller periods\n"
+ "    and CAN Ids.  This flag\n"
+ "    causes all other options to be ignored.  The recommended way to use this is\n"
+ "    to save the output into a file which is included in unit and integration\n"
+ "    tests using the #INCLUDE directive.  This will simplify your update task\n"
+ "    if you change CAN IDs or controller periods later on.\n"
+ "\n"
+ "  -cf filename\n"
+ "    Do a unit/integration test.  <filename> is a list of controllers to \n"
+ "    instantiate.  Each line should be of the form\n"
+ "\n"
+ "        ControllerClassName param param ...\n"
+ "\n"
+ "    On each line, all text after and including the first semi-colon will be\n"
+ "    ignored.  The <param> will be parsed as arguments to the appropriate\n"
+ "    constructor.  A message file must also be specified (see -mf).\n"
+ "\n"
+ "    Also, for constructors which take types such as Hallway or Side\n"
+ "    you can use the enumeration constant as the value in your .cf file.\n"
+ "    For example, for a Hallway param, you can use FRONT or BACK.\n"
+ "\n"
+ "    The cf file also supports the #INCLUDE syntax.  See the -mf section for\n"
+ "    details.\n"
+ "\n"
+ "  -mf filename\n"
+ "    Only valid when -cf is specified.  Each line of <filename> describes a \n"
+ "    message to inject or an assertion to check.  \n"
+ "    Injections are inputs to the system, assertions check outputs.\n"
+ "\n"
+ "    Physical message injections should be of the form\n"
+ "        time  type  period  context  payload  param param ... = arg arg ...\n"
+ "\n"
+ "    and network messages should be of the form\n"
+ "        time  type  period  context  msgid  translator  param param ... = arg arg ...\n"
+ "\n"
+ "    and assertions on physical messages should have the form \n"
+ "        time  type  context  payload param param ... : member operator value \n"
+ "\n"
+ "    and assertions on network messages messages should have the form \n"
+ "        time  type  context msgid translator param param ... : member operator value \n"
+ "\n"
+ "    and assertions on controller state should have the form \n"
+ "        time  type  controllername : key operator value\n"
+ "    (see the documentation for Controller.checkState() for more details on\n"
+ "    state assertions.)\n"
+ "\n"
+ "    On each line, all text after and including the first semi-colon will be\n"
+ "    ignored.\n"
+ "    - <time> is the time at which to send the message. This should be in \n"
+ "             SimTime format (i.e. <value><unit>, where unit can be s, ms, etc.) \n"
+ "             Also, you can choose for this value to be an increment from the \n"
+ "             previous message, or an absolute time to fire the event. By preceeding \n"
+ "             the time with a '+' it will increment from the previous time. \n"
+ "             For example, if the first event fires at time 5s and the second fires \n"
+ "             at time +2s, the second event will fire at absolute time 7s. \n"
+ "    - <type> is I for injected message, A for assertion, or S for controller state.\n"
+ "    - <period> is a time interval between subsequent messages.  A zero <period>\n"
+ "      will make the message not repeat.\n"
+ "    - <context> is F or N, for framework or network message.\n"
+ "    - <payload> is the type of the message.  For example, AtFloor or\n"
+ "      DesiredFloor.  The string \"Payload\" will be appended to the end of\n"
+ "      <payload> to make a class name (which must exist in simulator.payloads).\n"
+ "      Note that even though you use a static factory method to create Readable\n"
+ "      and Writeable payloads in your code, this item in the test file should\n"
+ "      refer to the outer payload class, e.g. \"HallCallPayload\", not\n"
+ "      \"ReadableHallCallPayload\" or \"HallCallPayload.getReadablePayload()\"\n"
+ "    - <msgid> is an integer, the CAN message ID for this message.  It can be given\n"
+ "      in hexadecimal format by prepending '0x'\n"
+ "    - <translator> is the type of translator to use with this message.  The\n"
+ "      string \"CanPayloadTranslator\" will be appended to the end of\n"
+ "      <translator> to make a class name (which must exist in\n"
+ "      simulator.elevatormodules, simulator.elevatorcontrol, or\n"
+ "      simulator.payloads.translators).\n"
+ "    - <params> will be passed to the constructor of <payload> or <translator>.\n"
+ "    - <args> will be passed to the set() method of the Payload or the translator\n"
+ "      after it is constructed.\n"
+ "    - <member> is the public field or method of the payload or translator that is \n"
+ "      checked by the assertion.  Methods must have no arguments and not be void.\n"
+ "    - <operator> one of ==, !=, <, >, <=, >=.  The value read from the member is\n"
+ "      the left operator, and the specified value is the right operator.  Equals and\n"
+ "      not equals can be used on all types.  A exception will be thrown is the\n"
+ "      comparison operators are used on a non-numeric type.\n"
+ "      For state assertions, only == and != are allowed, and the comparison is.\n"
+ "      a case-sensitive string comparison.\n"
+ "    - <value> must be a value that is compatible with the <member> type (for fields)\n"
+ "      or return value (for methods).  At the appointed time, the specified value will be\n"
+ "      compared to member value and the assertion will indicate whether it passes\n"
+ "      or fails by printing the results.\n"
+ "    - <controllername> is the name of the controller whose state you wish to check.\n"
+ "    - <statevalue> a list of parameters that will be passed to the checkState() \n"
+ "      method of the controller\n"
+ "\n"
+ "    Note: \"=\" must separate the constructor parameters from the\n"
+ "    the arguments to set().\n"
+ "\n"
+ "    Message Injector Macro Feature:\n"
+ "\n"
+ "    The syntax for the macro is:\n"
+ "      #DEFINE <macro> <value>\n"
+ "    Whereever the <macro> token appears in the test file, <value> will\n"
+ "    automatically be substituted.  This is especially useful\n"
+ "    for values which might change, like CAN message IDs and message periods.\n"
+ "    Note:  the macro is a single token replacement, so you cannot use a macro\n"
+ "    to replace multiple fields in the message injector file.\n"
+ "\n"
+ "    Message Injector Include Feature:\n"
+ "\n"
+ "    The syntax for the include directive is:\n"
+ "      #INCLUDE <newfile>\n"
+ "    the contents of newfile will be parsed inline at the point where the\n"
+ "    #INCLUDE line is placed in the file.  Including a file in itself, or\n"
+ "    creating a series of include files that form a cycle will result in an\n"
+ "    error.\n"
+ "\n"
+ "\n"
+ "  -pf filename\n"
+ "    Do an acceptance test.  All controllers, sensors, and actuators will be\n"
+ "    instantiated.  Passenger information will be read from <filename>.\n"
+ "    Each line should be of the form\n"
+ "\n"
+ "        time startFloor startHallway endFloor endHallway\n"
+ "\n"
+ "    All text after and including the first semi-colon will be ignored.\n"
+ "    - <time>, <startFloor>, and <endFloor> have the obvious meanings.\n"
+ "             Also, you can choose for the <time> value to be an increment from the \n"
+ "             previous message, or an absolute time to fire the event. By preceeding \n"
+ "             the time with a '+' it will increment from the previous time. \n"
+ "             For example, if the first event fires at time 5s and the second fires \n"
+ "             at time +2s, the second event will fire at absolute time 7s. \n"
+ "    - Floor 1 is the lowest floor in the simulation.\n"
+ "    - If <startFloor> is 0, the passenger will start inside the car. In this\n"
+ "      case, the <startHallway> parameter should be specified even though it is\n"
+ "      ignored.\n"
+ "    - <startHallway> and <endHallway> must be FRONT or BACK, but the value for\n"
+ "      <startHallway> may be different from <endHallway>.\n"
+ "\n"
+ "    The passenger file also supports the #INCLUDE syntax.  See the -mf section\n"
+ "    for details.\n"
+ "\n"
+ "  -il\n"
+ "    When present passengers will ignore the requirement that the elevator car\n"
+ "    must be level with a floor for the passengers to enter. Only effective\n"
+ "    during acceptance tests.\n"
+ "\n"
+ "  -ff filename\n"
+ "    Inject faults into the simulation.  The faults are read from <filename>.\n"
+ "    \n"
+ "    The fault file also supports the #INCLUDE syntax.  See the -mf section\n"
+ "    for details.\n"
+ "\n"
+ "    The fault file name contains one fault type on each row.  Different fault\n"
+ "    types require different arguments.  The fault types are summarized here:\n"
+ "    \n"
+ "    Network DropMessages <DropPercentage>\n"
+ "       This model randomly drops messages from the CAN network at a rate\n"
+ "       specified by DropPercentage.\n"
+ "       This fault will not drop two messages in a row of any type\n"
+ "       DropPercentage - must be a number in the range 0-100.  \n"
+ "    \n"
+ "    FailedButton <StartTime> <Duration> <HallCall|CarCall> <Location>\n"
+ "       This model causes the specified button to fail silent.\n"
+ "       StartTime - a time string representing the time to start the button\n"
+ "           failure.\n"
+ "       Duration - a time string specifying how long the fault should last.\n"
+ "           use \"FOREVER\" for a permanent fault.\n"
+ "       HallCall|CarCall - specify which button type fails.  Specify only one.\n"
+ "       Location - series of space-separated arguments that describe which\n"
+ "           button has failed.\n"
+ "           For Car calls, it is FLOOR SIDE.\n"
+ "           For Hall calls, it is FLOOR SIDE DIRECTION.\n"
+ "    \n"
+ "    DoorMotorBlackout <duration>\n"
+ "       This fault causes a total blackout on the CAN network for the specified\n"
+ "       duration every time the door motors are activated to open or close the\n"
+ "       doors.\n"
+ "       Duration - how long each blackout lasts.\n"
+ "\n"
+ "   -monitor <MonitorName>\n"
+ "     Locates and instantiates a class named MonitorName in the elevatorcontrol \n"
+ "     or elevatormodules package.  Note that monitors submitted by students  \n"
+ "     must be located in the elevatorcontrol package.\n"
+ "     The monitor class must be descended from simulator.framework.RuntimeMonitor\n"
+ "     and must have a constructor that takes no arguments.\n"
+ "\n"
+ "  -gui\n"
+ "    Display a realtime graphical representation of the simulation.  For detailed\n"
+ "    passenger information, hover the mouse over the passenger counts for the\n"
+ "    hall landings or car.  A complete list of passengers if visible in the\n"
+ "    tooltip.  This feature works best if the simulation is paused or running\n"
+ "    very slowly.\n"
+ "\n"
+ "  -rt time\n"
+ "    - For acceptance tests: the amount of time to run the simulator after\n"
+ "      the last passenger is injected.  Default value is 1 hour.\n"
+ "    - For unit/integration tests: the amoung of time to run the test\n"
+ "      test after the last message is injected into the network.\n"
+ "      default is 5 seconds.\n"
+ "\n"
+ "  -rate R\n"
+ "    (Only valid when the GUI is enabled.)  Sets the initial execution speed of\n"
+ "    the simulator to a multiple of realtime.  For every second that passes in\n"
+ "    real time, R seconds will pass in the simulator.  If R is 1.0, the simulator\n"
+ "    will try to execute in realtime.  If R is INFINITE, the simulator will run\n"
+ "    as quickly as it can.  Note that this only constrains the maximum speed of\n"
+ "    the simulator.  Other factors (such as lots of printed output) may slow the\n"
+ "    simulator significantly.\n"
+ "\n"
+ "    If the -rate flag is not specified, the default values are INFINITE if the\n"
+ "    GUI is disabled and 1.0 if the GUI is enabled.\n"
+ "\n"
+ "  -break BREAKPOINTLIST\n"
+ "    (Only valid when the GUI is enabled.)  BREAKPOINTLIST shall be a\n"
+ "    comma-separated list of breakpoint times.  Each breakpoint pauses the\n"
+ "    simulator execution (drops the runtimerate to 0).  Each breakpoint occurs\n"
+ "    before any simulation events scheduled at that time.\n"
+ "\n"
+ "  -seed RANDOMSEED\n"
+ "    Specify a random seed for the simulation run.  All the 'random' behavior in\n"
+ "    the simulator is pseudorandom, so specifying a particular random seed should\n"
+ "    make the simulation run repeatable.  If the -seed flag is omitted, the\n"
+ "    system timestamp is used.  The random seed for each run is printed at the\n"
+ "    beginning of the run.\n"
+ "\n"
+ "  -head HEADERFILE\n"
+ "    HEADERFILE specifies a path to a header file.  The contents of this file\n"
+ "    will be printed to the console to at the beginning of the simulation and\n"
+ "    into the elevator.stats file after an acceptance test.\n"
+ "\n"
+ "  -b N\n"
+ "    Set the bandwidth to N*1000 bits/sec.  N must be a positive integer. The\n"
+ "    default is 200 kbps.\n"
+ "\n"
+ "  -fs FASTSPEED\n"
+ "    Specify the FASTSPEED of the elevator in m/s.  Default is 1.0 m/s.  This\n"
+ "    option accepts any value in the range [1.0, 10.0].\n"
+ "\n"
+ "  -cv\n"
+ "    Get verbose output from the Controller factory.  If you want verbose output\n"
+ "    from a Control object, have the controller's constructor accept a verbosity\n"
+ "    parameter, and then change the control file (see the -cf option).\n"
+ "\n"
+ "  -nv\n"
+ "    Get verbose output from the network modules.\n"
+ "\n"
+ "  -fnv\n"
+ "    Get verbose output from the framework messages.\n"
+ "\n"
+ "  -fv\n"
+ "    Get verbose output from the framework objects (such as the\n"
+ "    AtFloor sensors).\n"
+ "\n"
+ "  -pv\n"
+ "    Get verbose output from the passengers.\n"
+ "\n"
+ "  -miv\n"
+ "    Get verbose output from the MessageInjector.\n"
+ "\n"
+ "  -fiv\n"
+ "    Get verbose output from the FaultInjector.\n"
+ "\n"
+ "  -dropv\n"
+ "    Get a lot of information about which messages are dropped.\n");

    }
}
