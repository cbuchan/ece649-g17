/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.framework.faults;

import jSimPack.SimTime;
import java.text.ParseException;

/**
 * static utility methods for faults
 * @author justinr2
 */
public class FaultUtility {

    

    public static double parseDouble(String valueStr) throws ParseException {
        return parseDouble(valueStr, 0, 0, false);
    }
    public static double parseDouble(String valueStr, double min, double max) throws ParseException {
        return parseDouble(valueStr, min, max, true);
    }

    private static double parseDouble(String valueStr, double min, double max, boolean useMinMax) throws ParseException {
        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not parse " + valueStr + " into a double: " + ex.getMessage(), 0);
        }
        if (useMinMax) {
            if (value < min || value > max) {
                throw new ParseException("Value " + value + " must be in the range [" + min + "," + max + "]", 0);
            }
        }
        return value;
    }

    public static long parseLong(String valueStr) throws ParseException {
        return parseLong(valueStr, 0, 0, false);
    }
    public static long parseLong(String valueStr, long min, long max) throws ParseException {
        return parseLong(valueStr, min, max, true);
    }

    private static long parseLong(String valueStr, long min, long max, boolean useMinMax) throws ParseException {
        long value;
        try {
            value = Long.parseLong(valueStr);
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not parse " + valueStr + " into a long: " + ex.getMessage(), 0);
        }
        if (useMinMax) {
            if (value < min || value > max) {
                throw new ParseException("Value " + value + " must be in the range [" + min + "," + max + "]", 0);
            }
        }
        return value;
    }
    
    public static boolean parseBoolean(String boolStr) throws ParseException {
        if (boolStr.trim().equalsIgnoreCase("TRUE")) {
            return true;
        } else if (boolStr.trim().equalsIgnoreCase("FALSE")) {
            return false;
        } else {
            throw new ParseException("Cannot parse " + boolStr + " into a boolean value", 0);
        }
    }

    
    public static SimTime parseTime(String timeStr) throws ParseException {
        SimTime time;
        try {
            time = new SimTime(timeStr);
        } catch (NumberFormatException ex) {
            throw new ParseException("Cannot parse " + timeStr + " into a time value: " + ex.getMessage(), 0);
        }
        return time;
    }

}
