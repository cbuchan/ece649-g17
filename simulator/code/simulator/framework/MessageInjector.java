package simulator.framework;

import jSimPack.SimTime;
//import simulator.payloads.translators.CanPayloadTranslator;
import java.lang.reflect.*;
import java.util.*;
import java.text.ParseException;

import java.util.Map.Entry;
import simulator.payloads.*;
import simulator.payloads.CanMailbox.ReadableCanMailbox;
import simulator.payloads.CanMailbox.WriteableCanMailbox;
import simulator.payloads.translators.CanPayloadTranslator;

/**
 * This class is responsible for parsing mf files and creating the objects that
 * do the actual message injections and assertion monitoring.
 *
 * Objects are constructed according to the text format described in the command line
 * documentation in Elevator.  It is important that any changes to this class also
 * result in changes to the documentation in Elevator.printFullUsage().
 * 
 * 
 */
public class MessageInjector extends Networkable implements TimeSensitive, Parser {

    public final static String DEFINE_DIRECTIVE = "#DEFINE";
    private ArrayList<String> assertionResults = new ArrayList<String>();
    private HashMap<String, String> macros = new HashMap<String, String>();
    private int assertionPassCount = 0;
    private int assertionFailCount = 0;
    private int assertionTotal = 0; // this is incremented when the assertions are created, not when they are executed.
    private final ControllerSet controllers; //all the controllers that have been instantiated - used for StateAssertionMonitors to get references to the controller objects.
    boolean verbose;
    ReflectionFactory refFactory;
    SimTime lastInjectionTime;
    NetworkConnection canNetwork;
    NetworkConnection physicalFramwork;

    /**
     * Reporting method for all assertion results.
     * @return  multiline string describing all assertion results.
     */
    public String getAssertionStats() {
        StringBuffer sb = new StringBuffer("Assertion Results:\n");
        for (String s : assertionResults) {
            sb.append(s);
            sb.append("\n");
        }
        sb.append("\n\n");
        sb.append(getAssertionSummary());
        return sb.toString();
    }

    /**
     * Reporting method that gives just pass/fail stats for assertions.
     * @return multiline string
     */
    public String getAssertionSummary() {
        StringBuffer sb = new StringBuffer("******************Summary******************\n");
        sb.append("Passed:  " + assertionPassCount + "\n");
        sb.append("Failed:  " + assertionFailCount + "\n");
        sb.append("Total :  " + assertionTotal + "\n");

        if (assertionTotal != assertionPassCount + assertionFailCount) {
            sb.append("\n  Some assertions were not executed, so you should probably increase the runtime.  \n");
        }
        return sb.toString();
    }

    /**
     * All monitors and injectable payloads implement Executable so they can be
     * executed when their scheduled time arrives.
     */
    private abstract class Executable {

        private final String lineInfo;
        public Executable(String lineInfo) {
            this.lineInfo = lineInfo;
        }

        public abstract void execute();
        public String getLineInfo() {
            return lineInfo;
        }
    }

    /**
     * Define a set of operators for comparison in assertions.
     */
    private enum Operator {

        EQUAL("==", false),
        NOT_EQUAL("!=", false),
        LESS_THAN("<", true),
        GREATER_THAN(">", true),
        LESS_THAN_OR_EQUAL("<=", true),
        GREATER_THAN_OR_EQUAL(">=", true);
        private String stringRep;
        private boolean requiresNumericTypes;

        private Operator(String stringRep, boolean requiresNumericTypes) {
            this.stringRep = stringRep;
            this.requiresNumericTypes = requiresNumericTypes;
        }

        public boolean getRequiresNumericTypes() {
            return requiresNumericTypes;
        }

        public String getStringRepresentation() {
            return stringRep;
        }

        public static Operator fromString(String operatorStr) {
            Operator operator = null;
            for (Operator o : Operator.values()) {
                if (o.getStringRepresentation().equals(operatorStr)) {
                    operator = o;
                    break;
                }
            }
            if (operator == null) {
                throw new IllegalArgumentException("\"" + operatorStr + "\" is not a recognized operator.");
            }
            return operator;
        }

        @Override
        public String toString() {
            return stringRep;
        }
    }

    /**
     * Helper class that injects a new network or framework message value with the specified period.
     */
    private class InjectablePayload extends Executable {

        /**
         * The message to inject.
         */
        final public WriteablePayload msg;
        /**
         * The interval between broadcasts.
         */
        final public SimTime period;
        /**
         * The network on which to broadcast this message.
         */
        final public MessageContext messageContext;

        public InjectablePayload(WriteablePayload msg, SimTime period, MessageContext messageContext, String lineInfo) {
            super(lineInfo);
            this.msg = msg;
            this.period = period;
            this.messageContext = messageContext;
        }

        @Override
        public String toString() {
            return "InjectablePayload:" + msg;
        }

        public void execute() {
            logPrint("injecting " + msg + " with period " + period);

            switch (messageContext) {
                case PHYSICAL:
                    if (period.isPositive()) {
                        logPrint("sending on FI");
                        physicalFramwork.sendTimeTriggered(msg, period);
                    } else {
                        logPrint("sending single event on FI");
                        physicalFramwork.sendOnce(msg);
                    }
                    break;
                case NETWORK:
                    if (period.isPositive()) {
                        logPrint("sending on CNI");
                        canNetwork.sendTimeTriggered(msg, period);
                    } else {
                        logPrint("sending single event on CNI");
                        canNetwork.sendOnce(msg);
                    }
                    break;
                default:
                    throw new RuntimeException("injected message context " + messageContext + " should be NETWORK or FRAMEWORK.");
            }
        }
    }

    /**
     * Helper class that checks the value of a network or framework message.
     */
    private class MessageAssertionMonitor extends Executable {

        final public Object target; //this is a translator or payload
        final public Method method;
        final public Operator operator;
        final public Field field;
        final public Object value;

        public MessageAssertionMonitor(Object target, Method method, Operator operator, Object value, String lineInfo) {
            super(lineInfo);
            this.target = target;
            this.method = method;
            this.field = null;
            this.operator = operator;
            this.value = value;
            if (method == null) {
                throw new NullPointerException("Method");
            }
            assertionTotal++;
        }

        public MessageAssertionMonitor(Object target, Field field, Operator operator, Object value, String lineInfo) {
            super(lineInfo);
            this.target = target;
            this.method = null;
            this.field = field;
            this.operator = operator;
            this.value = value;
            if (field == null) {
                throw new NullPointerException("Field");
            }
            assertionTotal++;
        }

        public void execute() {
            logPrint("Testing assertion " + target + " = " + value);
            if (method != null) {
                try {
                    testValues(method.invoke(target, (Object[]) null), value);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Could not find method " + method + " on " + target + ": " + ex);
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Could not find method " + method + " on " + target + ": " + ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException("Could not find method " + method + " on " + target + ": " + ex);
                }
            }
            if (field != null) {
                try {
                    testValues(field.get(target), value);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Could not find field " + field + " on " + target + ": " + ex);
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Could not find field " + field + " on " + target + ": " + ex);
                }
            }
        }

        /**
         * perform a operator b
         * @param a
         * @param b
         */
        private void testValues(Object a, Object b) {
            switch (operator) {
                case EQUAL:
                    if (a.equals(b)) {
                        pass();
                    } else {
                        fail();
                    }
                    return;
                case NOT_EQUAL:
                    if (!a.equals(b)) {
                        pass();
                    } else {
                        fail();
                    }
                    return;
            }
            //if we get this far, we have a numeric type
            int comparison = 0;
            if ((a instanceof Double) && (b instanceof Double)) {
                comparison = ((Double) a).compareTo((Double) b);
            } else if ((a instanceof Integer) && (b instanceof Integer)) {
                comparison = ((Integer) a).compareTo((Integer) b);
            } else if ((a instanceof Byte) && (b instanceof Byte)) {
                comparison = ((Byte) a).compareTo((Byte) b);
            } else if ((a instanceof Float) && (b instanceof Float)) {
                comparison = ((Float) a).compareTo((Float) b);
            } else if ((a instanceof Long) && (b instanceof Long)) {
                comparison = ((Long) a).compareTo((Long) b);
            } else if ((a instanceof Short) && (b instanceof Short)) {
                comparison = ((Short) a).compareTo((Short) b);
            } else {
                throw new RuntimeException("Could not convert values " + a + " and " + b + " for operator " + operator.getStringRepresentation());
            }
            switch (operator) {
                case GREATER_THAN:
                    if (comparison > 0) {
                        pass();
                    } else {
                        fail();
                    }
                    break;
                case GREATER_THAN_OR_EQUAL:
                    if (comparison >= 0) {
                        pass();
                    } else {
                        fail();
                    }
                    break;
                case LESS_THAN:
                    if (comparison < 0) {
                        pass();
                    } else {
                        fail();
                    }
                    break;
                case LESS_THAN_OR_EQUAL:
                    if (comparison <= 0) {
                        pass();
                    } else {
                        fail();
                    }
                    break;
                default:
                    break;
            }
        }

        private void pass() {
            String s = "PASSED " + target + " :: " + getMemberName() + " ?" + operator.getStringRepresentation() + " " + value + " PASSED";
            String m = fillString('*', s.length());
            //String marker = new StringBuffer()
            assertionResults.add("@" + Harness.getTime() + ":  " + s);
            assertionPassCount++;
            Harness.log("AssertionMonitor", m);
            Harness.log("AssertionMonitor", s);
            Harness.log("AssertionMonitor", m);
        }

        private String fillString(char c, int length) {
            StringBuffer b = new StringBuffer(length);
            for (int i = 0; i < length; i++) {
                b.append(c);
            }
            return b.toString();
        }

        private void fail() {
            String s = "FAILED " + target + " :: " + getMemberName() + " ?" + operator.getStringRepresentation() + " " + value + " FAILED";
            String m1 = fillString('/', s.length());
            String m2 = fillString('\\', s.length());
            assertionFailCount++;
            assertionResults.add("@" + Harness.getTime() + ":  " + s);
            Harness.log("AssertionMonitor", m1);
            Harness.log("AssertionMonitor", s);
            Harness.log("AssertionMonitor", m2);
            //throw new RuntimeException("Assertion " + target + " = " + value + " FAILED");
        }

        private String getMemberName() {
            if (method != null) {
                return method.getName();
            } else {
                return field.getName();
            }
        }
    }

    /**
     * Helper class that checks the state of a controller.
     */
    private class StateAssertionMonitor extends Executable {

        private final Controller target;
        private final String key;
        private final Operator operator;
        private final String testValue;

        public StateAssertionMonitor(Controller target, String key, Operator operator, String testValue, String lineInfo) {
            super(lineInfo);
            this.target = target;
            this.key = key;
            this.operator = operator;
            this.testValue = testValue;
            assertionTotal++;
        }

        public void execute() {
            logPrint("Testing assertion " + target + " " + key + operator.toString() + testValue);
            String value = target.checkState(key);
            boolean passed;
            switch (operator) {
                case EQUAL:
                    passed = value.equals(testValue);
                    break;
                case NOT_EQUAL:
                    passed = !value.equals(testValue);
                    break;
                default:
                    throw new RuntimeException("Operator " + operator + " not allowed here.");
            }
            if (passed) {
                pass(value);
            } else {
                fail(value);
            }
        }

        private void pass(String value) {
            String s;
            if (operator == Operator.EQUAL) {
                s = "PASSED " + target + "::checkState(" + key + ") ?" + operator.toString() + " " +  testValue + " PASSED";
            } else {
                s = "PASSED " + target + "::checkState(" + key + ") ?" + operator.toString() + " " +  testValue + " (" + value + " instead) PASSED";
            }
            String m = fillString('*', s.length());
            //String marker = new StringBuffer()
            assertionResults.add("@" + Harness.getTime() + ":  " + s);
            assertionPassCount++;
            Harness.log("AssertionMonitor", m);
            Harness.log("AssertionMonitor", s);
            Harness.log("AssertionMonitor", m);
        }

        private void fail(String actualValue) {
            String s = "FAILED " + target + "::checkState(" + key + ") ?" + operator.toString() + " " + testValue + " (" + actualValue + " instead) FAILED";
            String m1 = fillString('/', s.length());
            String m2 = fillString('\\', s.length());
            assertionFailCount++;
            assertionResults.add("@" + Harness.getTime() + ":  " + s);
            Harness.log("AssertionMonitor", m1);
            Harness.log("AssertionMonitor", s);
            Harness.log("AssertionMonitor", m2);
            //throw new RuntimeException("Assertion " + target + " = " + value + " FAILED");
        }
    }

    /**
     * 
     * @param filename name of the message file to parse
     * @param controllers the controllers instantiated in the system
     * @param verbose if true, print lots of messages about message injections
     */
    public MessageInjector(String filename, ControllerSet controllers, boolean verbose) {
        this.verbose = verbose;
        this.controllers = controllers;
        lastInjectionTime = SimTime.ZERO;
        canNetwork = Harness.getCANNetwork().getFrameworkConnection(this);
        physicalFramwork = Harness.getPhysicalNetwork().getFrameworkConnection(this);

        refFactory = new ReflectionFactory(verbose);

        FileTokenizer fp = new FileTokenizer(filename, verbose, this);
        fp.parseFile();
    }

    /**
     * 
     * @return the last time a message was injected.
     */
    public SimTime lastInjectionTime() {
        return lastInjectionTime;
    }

    /**
     * Helper class to assist in handling the string arguments in the parse method.
     * This class helps you to step through the tokens in a line, peek at the next
     * token, etc.  An internal variable keeps track of the "current word".
     */
    private class ParseTracker {

        public String[] words;
        public int currentWord = 0;

        public ParseTracker(String[] words) {
            this.words = words;
        }

        /**
         * 
         * @return the next work and advance the word pointer
         * @throws ParseException
         */
        public String getNextWord() throws ParseException {
            if (!hasNextWord()) {
                throw new ParseException("Not enough arguments in " + words, currentWord);
            }
            String retval = words[currentWord];
            currentWord++;
            return retval;
        }

        /**
         * 
         * @return the previous word without changing the word pointer
         */
        public String getPreviousWord() {
            return words[currentWord - 1];
        }

        /**
         * 
         * @return the next word without changing the word pointer
         */
        public String peekNextWord() {
            return words[currentWord];
        }

        /**
         * advance the word pointer
         */
        public void increment() {
            currentWord++;
        }

        /**
         * true if there are more words after the current word
         * @return
         */
        public boolean hasNextWord() {
            return (currentWord < words.length);
        }

        /**
         * Incremetns currentWord pointer if compare successful, otherwise just 
         * return false and don't change the word pointer
         */
        public boolean compareNextWord(String compareTo) {
            if (words[currentWord].equals(compareTo)) {
                currentWord++;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Contructs one of the helper objects classes (InjectablePayload, MessageAssertionMonitor,StateAssertionMonitor)
     * based on the contents of the line, then queues it for execution.
     *
     */
    public void parse(String[] words, FileTokenizer sourceFT) throws ParseException {
    	boolean timeIsAbsolute = true;
    	
        logPrint(sourceFT.lineMessage("parsing " + Arrays.toString(words)));

        if (words[0].equals(DEFINE_DIRECTIVE)) {
            if (words.length != 3) {
                throw new ParseException("Expected two arguments for a #DEFINE macro.", 0);
            }
            //add the macro to the list
            macros.put(words[1], words[2]);
            return;
        }

        if (words.length < 4) {
            throw new ParseException(words.length
                    + " is not enough parameters.  Need at least four (time, period, N or F, type).", 0);
        }

        //do macro replacements
        if (macros.size() > 0) {
            for (int i = 0; i < words.length; i++) {
                for (Entry<String, String> macro : macros.entrySet()) {
                    if (words[i].equals(macro.getKey())) {
                        words[i] = macro.getValue();
                        break;
                    }
                }
            }
        }

        ParseTracker pt = new ParseTracker(words);

        //is injection time an increment or an absolute time?
        if (pt.peekNextWord().startsWith("+")){
        	timeIsAbsolute = false;
        }
        else{
        	timeIsAbsolute = true;
        }
        
        SimTime injectionTime = new SimTime(pt.getNextWord());

        if (injectionTime.isNegative()) {
            throw new ParseException("injection time must not be negative.", 0);
        }
        
        //if injection time is an increment, perform the increment
        if (!timeIsAbsolute){
        	injectionTime = SimTime.add(injectionTime, lastInjectionTime);
        }

        /* wbs
         * Should we do something if injectionTime is LESS than the previous time?
         */
        if (injectionTime.isGreaterThan(lastInjectionTime)) {
            lastInjectionTime = injectionTime;
        }

        logPrint(sourceFT.lineMessage("injection time: " + injectionTime));

        if (pt.compareNextWord("I")) {
            createInjectablePayload(pt, injectionTime, sourceFT);
        } else if (pt.compareNextWord("A")) {
            createAssertionMonitor(pt, injectionTime, sourceFT);
        } else {
            throw new ParseException(
                    "expected type \"A\" or \"I\", received " + pt.getNextWord(), 0);
        }
    }

    /**
     * parse the words to create a message injected into the system
     * @param pt
     * @param injectionTime
     * @param sourceFT
     * @throws ParseException
     */
    private void createInjectablePayload(ParseTracker pt, SimTime injectionTime, FileTokenizer sourceFT) throws ParseException {
        SimTime period;
        try {
            period = new SimTime(pt.getNextWord());
        } catch (NumberFormatException ex) {
            throw new ParseException(sourceFT.lineMessage("Could not parse " + pt.getPreviousWord() + " into a time format: " + ex), pt.currentWord - 1);
        }

        logPrint(sourceFT.lineMessage("period: " + period));

        if (period.isNegative()) {
            throw new ParseException(sourceFT.lineMessage("period: " + period + " must be non-negative"), pt.currentWord - 1);
        }

        if (pt.compareNextWord("F")) {
            createInjectableFrameworkPayload(pt, injectionTime, period, sourceFT);
        } else if (pt.compareNextWord("N")) {
            createInjectableNetworkPayload(pt, injectionTime, period, sourceFT);
        } else {
            throw new ParseException(
                    sourceFT.lineMessage("expected network type \"F\" or \"N\", received " + pt.getNextWord()),
                    pt.currentWord);
        }


    }

    private void createInjectableFrameworkPayload(ParseTracker pt, SimTime injectionTime, SimTime period, FileTokenizer sourceFT) throws ParseException {
        List<String> packagePath = new ArrayList<String>();
        List<String> payloadArgs = new ArrayList<String>();
        List<String> setArgs = new ArrayList<String>();

        MessageContext messageContext = MessageContext.PHYSICAL;

        String payloadClassName = pt.getNextWord() + "Payload";
        packagePath.add("simulator.payloads.");

        //the parameters up to the equal sign are arguments for the payload static factory method
        while (pt.hasNextWord()) {
            if (pt.compareNextWord("=")) {
                break;
            }
            payloadArgs.add(pt.getNextWord());
        }

        if (!pt.getPreviousWord().equals("=")) {
            throw new ParseException(sourceFT.lineMessage("Expected an = symbol at the end of payload parameters and before the value list."), pt.currentWord - 1);
        }

        //put the rest of the arguments into the setArgs list
        while (pt.hasNextWord()) {
            setArgs.add(pt.getNextWord());
        }

        if (setArgs.isEmpty()) {
            throw new ParseException(sourceFT.lineMessage("Expected at least one value parameter after the = symbol."), pt.currentWord - 1);
        }

        logPrint(sourceFT.lineMessage("Payload name: " + payloadClassName));
        logPrint(sourceFT.lineMessage("Payload args: " + ReflectionFactory.commaConcatenate(payloadArgs)));
        logPrint(sourceFT.lineMessage("set() args: " + ReflectionFactory.commaConcatenate(setArgs)));

        WriteablePayload payload = null;
        try {
            payload = (WriteablePayload) refFactory.createFromFactoryMethod(payloadClassName, packagePath, "getWriteablePayload", payloadArgs);
        } catch (Exception ex) {
            String error = "Unable to construct a framework message with this line: " + ex + "\n"
                    + "Payload name: " + payloadClassName + "\n"
                    + "Payload args: " + ReflectionFactory.commaConcatenate(payloadArgs);
            throw new RuntimeException(sourceFT.lineMessage(error));
        }

        invokeSetMethod(refFactory, payload, setArgs, sourceFT);

        InjectablePayload imp =
                new InjectablePayload(payload, period, messageContext, sourceFT.getLineInfo());

        new SystemTimer(this).start(injectionTime, imp);
    }

    private void createInjectableNetworkPayload(ParseTracker pt, SimTime injectionTime, SimTime period, FileTokenizer sourceFT) throws ParseException {
        List<String> packagePath = new ArrayList<String>();
        String translatorName;
        List<String> translatorArgs = new ArrayList<String>();
        List<String> setArgs = new ArrayList<String>();

        MessageContext messageContext = MessageContext.NETWORK;

        //locations to look for translators
        packagePath.add("simulator.elevatorcontrol.");
        packagePath.add("simulator.elevatormodules.");
        packagePath.add("simulator.payloads.translators.");

        //get the CAN ID
        int canID;
        try {
            if (pt.peekNextWord().startsWith("0x")) {
                canID = Integer.parseInt(pt.getNextWord().substring(2), 16);
            } else {
                canID = Integer.parseInt(pt.getNextWord());
            }
        } catch (NumberFormatException e) {
            throw new ParseException(sourceFT.lineMessage(
                    "Error parsing network injection.  Expected integer for message type, received " + pt.getPreviousWord()), pt.currentWord - 1);
        }

        translatorName = pt.getNextWord() + "CanPayloadTranslator";

        //the parameters up to the equal sign are arguments for the payload static factory method
        while (pt.hasNextWord()) {
            if (pt.compareNextWord("=")) {
                break;
            }
            translatorArgs.add(pt.getNextWord());
        }

        if (!pt.getPreviousWord().equals("=")) {
            throw new ParseException(sourceFT.lineMessage("Expected an = symbol at the end of payload parameters and before the value list."), pt.currentWord - 1);
        }

        //put the rest of the arguments into the setArgs list
        while (pt.hasNextWord()) {
            setArgs.add(pt.getNextWord());
        }

        if (setArgs.isEmpty()) {
            throw new ParseException(sourceFT.lineMessage("Expected at least one value parameter after the = symbol."), pt.currentWord - 1);
        }

        logPrint(sourceFT.lineMessage("CAN ID: " + canID));
        logPrint(sourceFT.lineMessage("Translator args: " + ReflectionFactory.commaConcatenate(translatorArgs)));
        logPrint(sourceFT.lineMessage("set() args: " + ReflectionFactory.commaConcatenate(setArgs)));

        WriteableCanMailbox mailbox = CanMailbox.getWriteableCanMailbox(canID);

        CanPayloadTranslator translator = null;
        try {
            String[] strings = new String[translatorArgs.size() + 1];
            Object[] parameters = new Object[translatorArgs.size() + 1];
            parameters[0] = mailbox;
            strings[0] = null;
            for (int i=1; i < strings.length; i++) {
                strings[i] = translatorArgs.get(i-1);
                parameters[i] = null;
            }
            translator = (CanPayloadTranslator)refFactory.createObjectFromMixed(translatorName, packagePath, parameters, strings);
        } catch (Exception ex) {
            String error = "Unable to construct a translator for this line. " + ex + "\n"
                    + "CAN ID: " + canID + "\n"
                    + "Translator name: " + translatorName + "\n"
                    + "Translator args: " + ReflectionFactory.commaConcatenate(translatorArgs);
            throw new RuntimeException(sourceFT.lineMessage(error));
        }

        invokeSetMethod(refFactory, translator, setArgs, sourceFT);

        InjectablePayload imp =
                new InjectablePayload(mailbox, period, messageContext, sourceFT.getLineInfo());

        new SystemTimer(this).start(injectionTime, imp);
    }

    private void invokeSetMethod(ReflectionFactory refFactory, Object targetObj, List<String> setArgs, FileTokenizer sourceFT) throws ParseException {
        try {
            Method setMethod = refFactory.getMethod(targetObj,
                    "set", setArgs);
            refFactory.invoke(setMethod,
                    targetObj,
                    setArgs);
        } catch (NoSuchMethodException e) {
            throw new ParseException(sourceFT.lineMessage("could not find "
                    + targetObj.getClass().getSimpleName()
                    + ".set() that accepts: " + ReflectionFactory.commaConcatenate(setArgs)), -1);
        } catch (SecurityException ex) {
            throw new ParseException(sourceFT.lineMessage("Security exception: " + ex + "\nwhile invoking "
                    + targetObj.getClass().getSimpleName()
                    + ".set() that accepts: " + ReflectionFactory.commaConcatenate(setArgs)), -1);
        } catch (Exception ex) {
            throw new ParseException(sourceFT.lineMessage("Exception: " + ex + "\nwhile invoking "
                    + targetObj.getClass().getSimpleName()
                    + ".set() that accepts: " + ReflectionFactory.commaConcatenate(setArgs)), -1);
        }
    }

    /**
     * parse the words to create an assertion monitor
     * @param pt
     * @param injectionTime
     * @param sourceFT
     * @throws ParseException
     */
    private void createAssertionMonitor(ParseTracker pt, SimTime injectionTime, FileTokenizer sourceFT) throws ParseException {
        if (!pt.hasNextWord()) {
            throw new ParseException(sourceFT.lineMessage("Expected a context \"A\""), pt.currentWord);
        }

        if (pt.compareNextWord("S")) {
            createStateAssertionMonitor(pt, injectionTime, sourceFT);
        } else if (pt.compareNextWord("F")) {
            createFrameworkMessageAssertionMonitor(pt, injectionTime, sourceFT);
        } else if (pt.compareNextWord("N")) {
            createNetworkMessageAssertionMonitor(pt, injectionTime, sourceFT);
        } else {
            throw new ParseException(
                    sourceFT.lineMessage("expected network type \"F\" or \"N\" or \"S\", received " + pt.getPreviousWord()), pt.currentWord - 1);
        }
    }

    private void createStateAssertionMonitor(ParseTracker pt, SimTime injectionTime, FileTokenizer sourceFT) throws ParseException {
        if (!pt.hasNextWord()) {
            throw new ParseException(sourceFT.lineMessage("Expected a controller name following \"S\""), pt.currentWord);
        }
        //controller
        Controller controller = null;
        try {
            controller = controllers.get(pt.getNextWord());
        } catch (IllegalArgumentException ex) {
            throw new ParseException(sourceFT.lineMessage(ex.getMessage()), pt.currentWord - 1);
        }
        
        //:
        if (!pt.getNextWord().equals(":")) {
            throw new ParseException(sourceFT.lineMessage("Expected : after controller name."),pt.currentWord-1);
        }

        //key
        if (!pt.hasNextWord()) {
            throw new ParseException(sourceFT.lineMessage("Expected a state key"), pt.currentWord);
        }
        String keyString = pt.getNextWord();

        //operator
        if (!pt.hasNextWord()) {
            throw new ParseException(sourceFT.lineMessage("Expected an operator after state key"), pt.currentWord);
        }
        String operatorString = pt.getNextWord();
        Operator operator = null;
        try {
            operator = Operator.fromString(operatorString);
        } catch (IllegalArgumentException ex) {
            throw new ParseException(sourceFT.lineMessage(ex.getMessage()), (pt.currentWord - 1));
        }
        if (operator != Operator.EQUAL && operator != Operator.NOT_EQUAL) {
            throw new ParseException(sourceFT.lineMessage("Operator " + operatorString + " not allowed here.  Only == or != allowed."), pt.currentWord-1);
        }

        //value
        if (!pt.hasNextWord()) {
            throw new ParseException(sourceFT.lineMessage("Expected a value after the operator"), pt.currentWord);
        }
        String valueString = pt.getNextWord();


        StateAssertionMonitor mon = new StateAssertionMonitor(controller, keyString, operator, valueString, sourceFT.getLineInfo());

        new SystemTimer(this).start(injectionTime, mon);
    }

    /**
     * parse the words to create an assertion monitor that checks a network message.
     * @param pt
     * @param injectionTime
     * @param sourceFT
     * @throws ParseException
     */
    private void createNetworkMessageAssertionMonitor(ParseTracker pt, SimTime injectionTime, FileTokenizer sourceFT) throws ParseException {
        String translatorName;
        List<String> packagePath = new ArrayList<String>();
        List<String> translatorArgs = new ArrayList<String>();

        MessageContext messageContext = MessageContext.NETWORK;

        //locations to look for translators
        packagePath.add("simulator.elevatorcontrol.");
        packagePath.add("simulator.elevatormodules.");
        packagePath.add("simulator.payloads.translators.");

        //get the CAN ID
        int canID;
        try {
            if (pt.peekNextWord().startsWith("0x")) {
                canID = Integer.parseInt(pt.getNextWord().substring(2), 16);
            } else {
                canID = Integer.parseInt(pt.getNextWord());
            }
        } catch (NumberFormatException e) {
            throw new ParseException(sourceFT.lineMessage(
                    "Error parsing network injection.  Expected integer for message type, received " + pt.getPreviousWord()), pt.currentWord - 1);
        }

        translatorName = pt.getNextWord() + "CanPayloadTranslator";

        //the parameters up to the equal sign are arguments for the payload static factory method
        while (pt.hasNextWord()) {
            if (pt.compareNextWord(":")) {
                break;
            }
            translatorArgs.add(pt.getNextWord());
        }

        if (!pt.getPreviousWord().equals(":")) {
            throw new ParseException(sourceFT.lineMessage("Expected a : symbol at the end of payload parameters and before the value list."), pt.currentWord - 1);
        }

        logPrint(sourceFT.lineMessage("CAN ID: " + canID));
        logPrint(sourceFT.lineMessage("Translator args: " + ReflectionFactory.commaConcatenate(translatorArgs)));

        ReadableCanMailbox mailbox = CanMailbox.getReadableCanMailbox(canID);

        CanPayloadTranslator translator = null;
        try {
            String[] strings = new String[translatorArgs.size() + 1];
            Object[] parameters = new Object[translatorArgs.size() + 1];
            parameters[0] = mailbox;
            strings[0] = null;
            for (int i=1; i < strings.length; i++) {
                strings[i] = translatorArgs.get(i-1);
                parameters[i] = null;
            }
            translator = (CanPayloadTranslator)refFactory.createObjectFromMixed(translatorName, packagePath, parameters, strings);
        } catch (Exception ex) {
            String error = "Unable to construct a translator for this line. " + ex + "\n"
                    + "CAN ID: " + canID + "\n"
                    + "Translator name: " + translatorName + "\n"
                    + "Translator args: " + ReflectionFactory.commaConcatenate(translatorArgs);
            throw new RuntimeException(sourceFT.lineMessage(error));
        }

        MessageAssertionMonitor monitor = parseMessageAssertionMonitorTest(pt, translator, sourceFT);

        canNetwork.registerTimeTriggered(mailbox);

        new SystemTimer(this).start(injectionTime, monitor);
    }

    /**
     * parse the words to create an assertion monitor that checks a framework message.
     * @param pt
     * @param injectionTime
     * @param sourceFT
     * @throws ParseException
     */
    private void createFrameworkMessageAssertionMonitor(ParseTracker pt, SimTime injectionTime, FileTokenizer sourceFT) throws ParseException {
        List<String> packagePath = new ArrayList<String>();
        List<String> payloadArgs = new ArrayList<String>();

        MessageContext messageContext = MessageContext.PHYSICAL;

        String payloadClassName = pt.getNextWord() + "Payload";
        packagePath.add("simulator.payloads.");

        //the parameters up to the equal sign are arguments for the payload static factory method
        while (pt.hasNextWord()) {
            if (pt.compareNextWord(":")) {
                break;
            }
            payloadArgs.add(pt.getNextWord());
        }

        if (!pt.getPreviousWord().equals(":")) {
            throw new ParseException(sourceFT.lineMessage("Expected a : symbol at the end of payload parameters and before the assertion test."), pt.currentWord - 1);
        }

        //the remaining arguments are the name of the member to check and the value to check

        if (!pt.hasNextWord()) {
        }

        logPrint(sourceFT.lineMessage("Payload name: " + payloadClassName));
        logPrint(sourceFT.lineMessage("Payload args: " + ReflectionFactory.commaConcatenate(payloadArgs)));

        ReadablePayload payload = null;
        try {
            payload = (ReadablePayload) refFactory.createFromFactoryMethod(payloadClassName, packagePath, "getReadablePayload", payloadArgs);
        } catch (Exception ex) {
            String error = "Unable to construct a framework message with this line: " + ex + "\n"
                    + "Payload name: " + payloadClassName + "\n"
                    + "Payload args: " + ReflectionFactory.commaConcatenate(payloadArgs);
            throw new RuntimeException(sourceFT.lineMessage(error));
        }

        MessageAssertionMonitor monitor = parseMessageAssertionMonitorTest(pt, payload, sourceFT);

        physicalFramwork.registerTimeTriggered(payload);

        new SystemTimer(this).start(injectionTime, monitor);
    }

    private MessageAssertionMonitor parseMessageAssertionMonitorTest(ParseTracker pt, Object targetObj, FileTokenizer sourceFT) throws ParseException {
        String memberStr = null;
        String opStr = null;
        String valueStr = null;

        try {
            memberStr = pt.getNextWord();
            opStr = pt.getNextWord();
            valueStr = pt.getNextWord();
        } catch (ParseException ex) {
            throw new ParseException(sourceFT.lineMessage("Error getting test values:  " + ex), pt.currentWord);
        }

        logPrint(sourceFT.lineMessage("test args: " + memberStr + " " + opStr + " " + valueStr));

        Operator operator = null;
        try {
            operator = Operator.fromString(opStr);
        } catch (IllegalArgumentException ex) {
            throw new ParseException(sourceFT.lineMessage(ex.getMessage()), (pt.currentWord - 1));
        }

        MessageAssertionMonitor mon = null;
        try {
            Method testMethod = refFactory.getMethod(targetObj, memberStr);
            Object testValue = refFactory.createReturnType(testMethod, valueStr);
            testNumericType(testMethod.getReturnType(), operator, sourceFT);
            mon = new MessageAssertionMonitor(targetObj, testMethod, operator, testValue, sourceFT.getLineInfo());
        } catch (NoSuchMethodException e) {
            logPrint(sourceFT.lineMessage("could not find method "
                    + memberStr + "()  on object " + targetObj));
        }

        if (mon == null) {
            //no method, so try a field
            try {
                Field testField = refFactory.getField(targetObj, memberStr);
                Object testValue = refFactory.createFieldType(testField, valueStr);
                testNumericType(testField.getType(), operator, sourceFT);
                mon = new MessageAssertionMonitor(targetObj, testField, operator, testValue, sourceFT.getLineInfo());
            } catch (NoSuchFieldException ex) {
                logPrint(sourceFT.lineMessage("could not find field "
                        + memberStr + " in object " + targetObj));
            }
        }

        if (mon == null) {
            throw new ParseException(sourceFT.lineMessage("Could not find a method or field named " + memberStr + " in " + targetObj), -1);
        }

        return mon;

    }



    /**
     * Utility method for checking types
     * @param t
     * @param o
     * @throws ParseException if the operator requires a numeric type, nad the specified type is not numeric.
     */
    private void testNumericType(Type t, Operator o, FileTokenizer sourceFT) throws ParseException {
        if (o.getRequiresNumericTypes()) {
            if (t != Double.TYPE
                    && t != Float.TYPE
                    && t != Integer.TYPE
                    && t != Long.TYPE
                    && t != Short.TYPE
                    && t != Byte.TYPE) {
                throw new ParseException(sourceFT.lineMessage("Operator " + o.getStringRepresentation() + " requires numeric type, not " + t + "."), 0);
            }
        }
    }

    /**
     * callback for scheduled helper objects.  
     * @param msg The helper object to be executed.  This object must implement Executable.
     */
    public void timerExpired(Object msg) {
        logPrint("enter timerExpired(" + msg + ")");

        Executable exec = (Executable) msg;
        try {
            exec.execute();
        } catch (Throwable ex) {
            throw new RuntimeException("Exception while executing " + exec.toString() + ", originally from " + exec.getLineInfo(), ex);
        }

    }

    private void logPrint(String s) {
        if (verbose) {
            Harness.log("MessageInjector", s);
        }
    }

    @Override
    public String toString() {
        return "MessageInjector";
    }

    /**
     * 
     * @param c
     * @param length
     * @return a string of length length composed of the character c.
     */
    private String fillString(char c, int length) {
        StringBuffer b = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            b.append(c);
        }
        return b.toString();
    }

    /**
     * 
     * @param strs
     * @return the string elements concatenated with commas between.
     */
    private String commaString(String[] strs) {
        StringBuilder b = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < strs.length; i++) {
            if (isFirst) {
                b.append(strs[i]);
                isFirst = false;
            } else {
                b.append(",");
                b.append(strs[i]);
            }
        }
        return b.toString();
    }
}
