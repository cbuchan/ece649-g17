/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jSimPack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class offers a units-aware represenation of simulation time.  It can be
 * used to represent a moment in time (i.e., a timestamp) or a duration.  Because
 * negative durations are valid, negative values are allowed.
 * @author Justin Ray
 */
public class SimTime implements Comparable<SimTime>, Cloneable {

    protected final long nativeTime;

    public static enum SimTimeUnit {

        NANOSECOND("ns", 1),
        MICROSECOND("us", 1000),
        MILLISECOND("ms", 1000000),
        SECOND("s", 1000000000),
        MINUTE("m", 60000000000L),
        HOUR("h", 3600000000000L);
        private String unitString;
        private long conversion;

        private SimTimeUnit(String unitString, long conversion) {
            this.unitString = unitString;
            this.conversion = conversion;
        }

        public String getUnitString() {
            return unitString;
        }
    }
    /**
     * Controls the unit produced by the toString method.
     */
    public static SimTimeUnit stringUnit = SimTimeUnit.SECOND;
    /**
     * A SimTime object representing zero, since this is a common constant
     */
    public final static SimTime ZERO = new SimTime(0);
    /**
     * Represents a time in the future that will never be reached.
     */
    public final static SimTime FOREVER = new SimTime(Long.MAX_VALUE);
    /**
     * This string table lists values that are translated into the SimTime.FOREVER value;
     */
    private final static String[] FOREVER_STRINGS = {
        "FOREVER",
        "INFINITE"
    };
    /**
     * This string table lists values that are translated into the SimTime.ZERO value;
     */
    private final static String[] ZERO_STRINGS = {
        "ZERO"
    };

    private SimTime(long nativeTime) {
        this.nativeTime = nativeTime;
    }

    /**
     * Creates a new SimTime object
     * @param timeValue  the amount time
     * @param unit time unit the value refers to
     */
    public SimTime(long scaledTime, SimTimeUnit unit) {
        nativeTime = scaledTime * unit.conversion;
    }

    public SimTime(double scaledTime, SimTimeUnit unit) {
        nativeTime = Math.round(scaledTime * unit.conversion);
    }

    /**
     * Creates a new SimTime object based on the input string.
     * The string should be of the form "<value><units>".  White space is allowed
     * but fractional values are not.  See the SimTimeUnit object for the valid
     * unit strings.
     * @param valueStr the string to convert
     */
    public SimTime(String valueStr) {
        nativeTime = parseTime(valueStr);
    }
    private static Pattern stringPatternLong = Pattern.compile("\\s*\\+?(\\d+)\\s*([a-zA-Z]+)\\s*");
    private static Pattern stringPatternDouble = Pattern.compile("\\s*\\+?(\\d+\\.\\d*)\\s*([a-zA-Z]+)\\s*");

    /**
     * convert a string containing a number and a unit
     * 
     * Note that the pattern used to vet the string conversion probably fails
     * if a negative value is passed.
     * @param valueStr The string to be converted
     */
    public long parseTime(String valueStr) {
        //check string constants first
        for (String s : FOREVER_STRINGS) {
            if (s.equalsIgnoreCase(valueStr.trim())) {
                return Long.MAX_VALUE;
            }
        }
        for (String s : ZERO_STRINGS) {
            if (s.equalsIgnoreCase(valueStr.trim())) {
                return 0;
            }
        }
        //try long
        Matcher m = stringPatternLong.matcher(valueStr);
        if (m.matches()) {
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2);
            for (SimTimeUnit u : SimTime.SimTimeUnit.values()) {
                if (u.getUnitString().equalsIgnoreCase(unit)) {
                    return value * u.conversion;
                }
            }
            throw new NumberFormatException("Units of '" + unit + "' not recognized in " + valueStr);
        }
        //try double
        m = stringPatternDouble.matcher(valueStr);
        if (m.matches()) {
            //simulator.framework.Harness.log("SimTime","matched!");
            double value = Double.parseDouble(m.group(1));
            String unit = m.group(2);
            for (SimTimeUnit u : SimTime.SimTimeUnit.values()) {
                if (u.getUnitString().equalsIgnoreCase(unit)) {
                    return (long) (value * (double) u.conversion);
                }
            }
            throw new NumberFormatException("Units of '" + unit + "' not recognized in " + valueStr);
        }
        throw new NumberFormatException("Cannot parse " + valueStr + " into a time format");
    }

    /**
     * Returns a truncated value (e.g. if native time is 2400 ns, then 
     * getTrunc(SimTimeUnit.MICROSECOND) will return 2, not 2.4.
     * 
     * @param unit The desired unit of the return value
     * @return the return value in the specified units
     */
    public long getTrunc(SimTimeUnit unit) {
        //according to java spec, dividing one integer by another returns the 
        //truncated result, so we do not need to truncate on our own.
        return nativeTime / unit.conversion;
    }

    //some convenience methods
    public long getTruncNanoseconds() {
        return getTrunc(SimTimeUnit.NANOSECOND);
    }

    public long getTruncMicroseconds() {
        return getTrunc(SimTimeUnit.MICROSECOND);
    }

    public long getTruncMilliseconds() {
        return getTrunc(SimTimeUnit.MILLISECOND);
    }

    public long getTruncSeconds() {
        return getTrunc(SimTimeUnit.SECOND);
    }

    public double getFracNanoseconds() {
        return getFrac(SimTimeUnit.NANOSECOND);
    }

    public double getFracMicroseconds() {
        return getFrac(SimTimeUnit.MICROSECOND);
    }

    public double getFracMilliseconds() {
        return getFrac(SimTimeUnit.MILLISECOND);
    }

    public double getFracSeconds() {
        return getFrac(SimTimeUnit.SECOND);
    }

    /**
     * Returns the floating point result, including any fractional units.
     * For example, if native time is 2400 ns, then getFrac(SimTimeUnit.MICROSECOND) 
     * will return 2.4.
     * 
     * @param unit The desired units
     * @return the floating point result in the specified units
     */
    public double getFrac(SimTimeUnit unit) {
        return (double) nativeTime / (double) unit.conversion;
    }

    public boolean isZero() {
        return nativeTime == 0;
    }

    public boolean isNonNegative() {
        return nativeTime >= 0;
    }

    public boolean isPositive() {
        return nativeTime > 0;
    }

    public boolean isNegative() {
        return nativeTime < 0;
    }

    public boolean isGreaterThan(SimTime compareTo) {
        return this.nativeTime > compareTo.nativeTime;
    }

    public boolean isGreaterThanOrEqual(SimTime compareTo) {
        return this.nativeTime >= compareTo.nativeTime;
    }

    public boolean isLessThan(SimTime compareTo) {
        return this.nativeTime < compareTo.nativeTime;
    }

    public boolean isLessThanOrEqual(SimTime compareTo) {
        return this.nativeTime <= compareTo.nativeTime;
    }

    public boolean isBefore(SimTime t) {
        return compareTo(t) < 0;
    }

    public boolean isAfter(SimTime t) {
        return compareTo(t) > 0;
    }

    /**
     * 
     * @return a string representation of the time formatted in the units specified
     * by SimTime.stringUnit
     */
    @Override
    public String toString() {
        return toString(stringUnit);
    }

    /**
     * 
     * @param units Units to display the time in 
     * @return a string containing the time formatted in the specified units
     */
    public String toString(SimTimeUnit units) {
        return Double.toString(getFrac(units)) + units.getUnitString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SimTime)) {
            return false;
        }
        return nativeTime == ((SimTime) obj).nativeTime;
    }

    @Override
    public int hashCode() {
        //according the the jdk documentation, this is the hashcode for a long
        return (int) (nativeTime ^ (nativeTime >>> 32));

    }

    public int compareTo(SimTime obj) {
        //after some testing, this is by far the fastest way to implement compare
        return Long.signum(nativeTime - obj.nativeTime);
    }

    /**
     * 
     * @param a first time value
     * @param b second time value
     * @return a new time object that is a+b
     */
    public static SimTime add(SimTime a, SimTime b) {
        return new SimTime(a.nativeTime + b.nativeTime);
    }

    /**
     * performs the subtraction (a - b)
     * @param a value to subtract from
     * @param b value to subtract
     * @return a new SimTim object with the time value (a-b)
     */
    public static SimTime subtract(SimTime a, SimTime b) {
        return new SimTime(a.nativeTime - b.nativeTime);
    }

    /**
     * multiply the time time value a * the scalar b and return a new SimTime object
     * @param a time value to be multiplied
     * @param b scalar multiplier
     * @return new SimTime object
     */
    public static SimTime multiply(SimTime a, long b) {
        return new SimTime(a.nativeTime * b);
    }

    //SimTime is immutable, so no reason to implement clone()
/*    @Override
    protected Object clone() throws CloneNotSupportedException {
    return new SimTime(this.nativeTime);
    }*/
//    private static class T1 implements Comparable<T1> {
//
//        long v;
//
//        public T1(long v) {
//            this.v = v;
//        }
//
//        public int compareTo(T1 o) {
//            if (v > o.v) {
//                return 1;
//            }
//            if (v < o.v) {
//                return -1;
//            }
//            return 0;
//        }
//    }
//
//    private static class T2 implements Comparable<T2> {
//
//        Long v;
//
//        public T2(long v) {
//            this.v = new Long(v);
//        }
//
//        public int compareTo(T2 o) {
//            return v.compareTo(o.v);
//        }
//    }
//
//    private static class T3 implements Comparable<T3> {
//
//        long v;
//
//        public T3(long v) {
//            this.v = v;
//        }
//
//        public int compareTo(T3 o) {
//            return Long.signum(v - o.v);
//        }
//    }
    public static void main(String[] args) {
//        T1 t1a = new T1(1);
//        T1 t1b = new T1(2);
//        T1 t1c = new T1(3);
//
//        T2 t2a = new T2(1);
//        T2 t2b = new T2(2);
//        T2 t2c = new T2(3);
//
//        T3 t3a = new T3(1);
//        T3 t3b = new T3(2);
//        T3 t3c = new T3(3);
//
//        long bound = 100000000;
//
//        long start;
//        long end;
//
//        start = System.currentTimeMillis();
//        for (long i = 0; i < bound; i++) {
//            t1a.compareTo(t1a);
//            t1a.compareTo(t1b);
//            t1a.compareTo(t1c);
//            t1b.compareTo(t1a);
//            t1b.compareTo(t1b);
//            t1b.compareTo(t1c);
//            t1c.compareTo(t1a);
//            t1c.compareTo(t1b);
//            t1c.compareTo(t1c);
//        }
//        end = System.currentTimeMillis();
//        System.out.println("T1: " + (double) (end - start) / 1000.0);
//
//        start = System.currentTimeMillis();
//        for (long i = 0; i < bound; i++) {
//            t2a.compareTo(t2a);
//            t2a.compareTo(t2b);
//            t2a.compareTo(t2c);
//            t2b.compareTo(t2a);
//            t2b.compareTo(t2c);
//            t2b.compareTo(t2b);
//            t2c.compareTo(t2a);
//            t2c.compareTo(t2b);
//            t2c.compareTo(t2c);
//        }
//        end = System.currentTimeMillis();
//        System.out.println("T2: " + (double) (end - start) / 1000.0);
//
//        start = System.currentTimeMillis();
//        for (long i = 0; i < bound; i++) {
//            t3a.compareTo(t3a);
//            t3a.compareTo(t3b);
//            t3a.compareTo(t3c);
//            t3b.compareTo(t3a);
//            t3b.compareTo(t3b);
//            t3b.compareTo(t3c);
//            t3c.compareTo(t3a);
//            t3c.compareTo(t3b);
//            t3c.compareTo(t3c);
//        }
//        end = System.currentTimeMillis();
//        System.out.println("T3: " + (double) (end - start) / 1000.0);
//        SimTime t1 = new SimTime(1, SimTimeUnit.SECOND);
//        SimTime t2 = new SimTime(2, SimTimeUnit.SECOND);
//        SimTime t3 = new SimTime(3, SimTimeUnit.SECOND);
//
//        Long l1 = new Long(1);
//        Long l2 = new Long(2);
//        Long l3 = new Long(3);
//
//        System.out.println("" + t1.compareTo(t2));
//        System.out.println("" + t1.compareTo(t3));
//        System.out.println("" + t2.compareTo(t1));
//        System.out.println("" + t2.compareTo(t3));
//        System.out.println("" + t3.compareTo(t1));
//        System.out.println("" + t3.compareTo(t2));
//
//        System.out.println();
//
//        System.out.println("" + l1.compareTo(l2));
//        System.out.println("" + l1.compareTo(l3));
//        System.out.println("" + l2.compareTo(l1));
//        System.out.println("" + l2.compareTo(l3));
//        System.out.println("" + l3.compareTo(l1));
//        System.out.println("" + l3.compareTo(l2));
//        SimTime t = new SimTime("1300us");
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.HOUR);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.MINUTE);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.SECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.MILLISECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.MICROSECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100,SimTimeUnit.NANOSECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.HOUR);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.MINUTE);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.SECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.MILLISECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.MICROSECOND);
//        System.out.println("SimTime " + t);
//
//        t = new SimTime(100.025,SimTimeUnit.NANOSECOND);
//        System.out.println("SimTime " + t);
//
//
//        t = new SimTime(100, SimTimeUnit.MICROSECOND);
//        SimTime t2 = new SimTime(.1, SimTimeUnit.MILLISECOND);
//
//        System.out.println("Equality Check: " + (t.equals(t2)));
//        System.out.println("Comparison: " + (t.compareTo(t2)));
//
//        t = new SimTime(100, SimTimeUnit.MICROSECOND);
//        t2 = new SimTime(120, SimTimeUnit.MICROSECOND);
//
//        System.out.println("Equality Check: " + (t.equals(t2)));
//        System.out.println("Comparison: " + (t.compareTo(t2)));
//
//        t = new SimTime(100, SimTimeUnit.MICROSECOND);
//        t2 = new SimTime(80, SimTimeUnit.MICROSECOND);
//
//        System.out.println("Equality Check: " + (t.equals(t2)));
//        System.out.println("Comparison: " + (t.compareTo(t2)));
//
//
//
//
//        System.out.println();
//
//
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//
    }
}
