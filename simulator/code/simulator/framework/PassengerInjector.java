package simulator.framework;

import jSimPack.SimTime;
import jSimPack.SimTime.SimTimeUnit;
import simulator.framework.PassengerController.Passenger;

import java.text.ParseException;
import java.io.*;
import java.util.*;

/**
 * Parses the -pf file and creates passenger objects accordingly.
 * @author justinr2
 */
public class PassengerInjector implements Parser, TimeSensitive {

    private boolean verbose;
    private final static SimTime period = new SimTime(10, SimTimeUnit.MILLISECOND);
    private final Set<Passenger> passengerPool;
    private final Set<Passenger> allPassengers;
    private final PassengerController passCon;
    private final SystemTimer passConTimer;
    private SimTime lastInjectionTime = SimTime.ZERO;

    public PassengerInjector(String filename, boolean verbose) {
        this.verbose = verbose;
        passengerPool = new LinkedHashSet<Passenger>();
        allPassengers = new LinkedHashSet<Passenger>();
        passCon = new PassengerController(verbose);
        passConTimer = new SystemTimer(this);
        passConTimer.start(period, passCon);

        new FileTokenizer(filename, verbose, this).parseFile();
    }

    public void parse(String[] words, FileTokenizer sourceFT) throws ParseException {
        if (words.length != 5) {
            throw new ParseException("received " + words.length +
                    " arguments, but needed 5: time, start floor," +
                    " start hallway, destination floor," +
                    " destination hallway.", 0);
        }

        SimTime injectionTime;

        try {
            injectionTime = new SimTime(words[0]);
        } catch (NumberFormatException e) {
            throw new ParseException(
                    "Invalid time \"" + words[0] + "\": " + e.getMessage(), 0);
        }

        if (injectionTime.isNegative()) {
            throw new ParseException(
                    "injection time must be zero or positive.", 0);
        }

        if (injectionTime.isAfter(lastInjectionTime)) {
            lastInjectionTime = injectionTime;
        }

        int startFloor, endFloor;
        Hallway startHallway, endHallway;

        try {
            startFloor = Integer.parseInt(words[1]);
        } catch (NumberFormatException e) {
            throw new ParseException(
                    "expected an integer for argument 2, received \"" +
                    words[1] + "\"", 0);
        }

        try {
            startHallway = Hallway.valueOf(words[2]);
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    "expected a Hallway for argument 3, received \"" +
                    words[2] + "\"", 0);
        }

        try {
            endFloor = Integer.parseInt(words[3]);
        } catch (NumberFormatException e) {
            throw new ParseException(
                    "expected an integer for argument 4, received \"" +
                    words[3] + "\"", 0);
        }

        try {
            endHallway = Hallway.valueOf(words[4]);
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    "expected a Hallway for argument 5, received \"" +
                    words[4] + "\"", 0);
        }

        /* the parser should throw checked exceptions for these conditions,
         * because bad input from user happens often.
         */

        // uncomment this test when the course staff say it's ok to do so.  Some
        // test files violate this condition, and it's dumb. --KSS 4/9/2008
        // if( startFloor == endFloor )
        //     throw new ParseException(
        //             "Passenger must be destined for a different floor than she started on.",0);
        if (startHallway != Hallway.FRONT && startHallway != Hallway.BACK) {
            throw new ParseException("Passenger must start in FRONT or BACK hallway.", 0);
        }
        if (endHallway != Hallway.FRONT && endHallway != Hallway.BACK) {
            throw new ParseException("Passenger must end in FRONT or BACK hallway.", 0);
        }

        Passenger passenger = passCon.makePerson(injectionTime,
                startFloor, startHallway, endFloor, endHallway);

        passengerPool.add(passenger);
        allPassengers.add(passenger);

        new SystemTimer(this).start(injectionTime, passenger);
    }

    /**
     * @return the latest passenger injection time
     */
    public SimTime getLastInjectionTime() {
        return lastInjectionTime;
    }

    public void timerExpired(Object data) {

        if (passCon == data) {
            passCon.run();
            if (passengerPool.isEmpty() && passCon.isEmpty()) {
                Harness.endSim();
            } else {
                passConTimer.start(period, passCon);
            }
            return;
        }

        // Time to inject a passenger.

        Passenger p = (Passenger) data;
        passengerPool.remove(p);
        log("injecting passenger " + p);
        p.start();
    }

    String getStats() {
        StringWriter output = new StringWriter();
        PrintWriter stats = new PrintWriter(output);

        
        stats.println("STATISTICS FOR ELEVATOR SIMULATION PASSENGER DELIVERY");
        stats.println("-----------------------------------------------------");
        stats.println();

        long temp_maxDeliveryTime = 0;
        long temp_totalDeliveryTime = 0;

        int delivered = 0;
        int stranded = 0;
        int notInjected = 0;

        stats.println("These passengers were delivered:");
        /* only consider passengers that were injected */
        allPassengers.removeAll(passengerPool);
        for (Passenger p : allPassengers) {
            if (p.isDelivered()) {
                ++delivered;
                stats.println(toString(p));
                temp_totalDeliveryTime += (p.getArrivalTime().getTruncMicroseconds() - p.getStartTime().getTruncMicroseconds());
                temp_maxDeliveryTime = Math.max(temp_maxDeliveryTime,
                        p.getArrivalTime().getTruncMicroseconds() - p.getStartTime().getTruncMicroseconds());
            }
        }
        SimTime totalDeliveryTime = new SimTime(temp_totalDeliveryTime, SimTimeUnit.MICROSECOND);
        SimTime maxDeliveryTime = new SimTime(temp_maxDeliveryTime, SimTimeUnit.MICROSECOND);

        if (delivered != allPassengers.size()) {
            stats.println();
            stats.println("Maybe you should increase the runtime, because\n" +
                    "these passengers were left stranded in the hall or in the car:");
            for (Passenger p : allPassengers) {
                if (!p.isDelivered()) {
                    stranded++;
                    stats.println(p);
                }
            }
        }

        if (!passengerPool.isEmpty()) {
            stats.println();
            stats.println(
                    "It is certain that you should increase the runtime," + " because\n" + " the injection time for these passengers was never reached:");
            for (Passenger p : passengerPool) {
                notInjected++;
                stats.println(p);
            }
        }

        double average = totalDeliveryTime.getFracSeconds() / (delivered);
        
        
        stats.println();
        stats.println("Passenger Delivery Summary");
        stats.println("Delivered: " + delivered);
        stats.println("Stranded: " + stranded);
        stats.println("Not_injected: " + notInjected);
        stats.println("Total: " + (allPassengers.size() + passengerPool.size()));
        
        if (notInjected == 0 && stranded == 0) {
            stats.println();
            stats.println("Performance Stats");
            stats.format("Average_delivery_time: %.3f", average);
            stats.println();
            stats.format("Maximum_delivery_time:  %.3f", maxDeliveryTime.getFracSeconds());
            stats.println();
            stats.format("Performance_score:  %.3f", (4 * average + (maxDeliveryTime.getFracSeconds())));
            stats.println();
        } else {
            stats.println();
            stats.println("Average_delivery_time: n/a");
            stats.println("Maximum_delivery_time: n/a");
            stats.println("Performance_score: " + Integer.MAX_VALUE);            
        }

        stats.close();
        return output.toString();
    }
    
    String getSummaryStats() {
        StringWriter output = new StringWriter();
        PrintWriter stats = new PrintWriter(output);

        long temp_maxDeliveryTime = 0;
        long temp_totalDeliveryTime = 0;

        int delivered = 0;
        int stranded = 0;
        int notInjected = 0;
        //stats.println("These passengers were delivered:");
        /* only consider passengers that were injected */
        allPassengers.removeAll(passengerPool);
        for (Passenger p : allPassengers) {
            if (p.isDelivered()) {
                ++delivered;
                //stats.println(toString(p));
                temp_totalDeliveryTime += (p.getArrivalTime().getTruncMicroseconds() - p.getStartTime().getTruncMicroseconds());
                temp_maxDeliveryTime = Math.max(temp_maxDeliveryTime,
                        p.getArrivalTime().getTruncMicroseconds() - p.getStartTime().getTruncMicroseconds());
            }
        }
        SimTime totalDeliveryTime = new SimTime(temp_totalDeliveryTime, SimTimeUnit.MICROSECOND);
        SimTime maxDeliveryTime = new SimTime(temp_maxDeliveryTime, SimTimeUnit.MICROSECOND);

        if (delivered != allPassengers.size()) {
            for (Passenger p : allPassengers) {
                if (!p.isDelivered()) {
                    stranded++;
                }
            }            
        }

        if (!passengerPool.isEmpty()) {           
            for (Passenger p : passengerPool) {
                notInjected++;
            }            
        }

        double average = totalDeliveryTime.getFracSeconds() / (delivered);

        stats.println();
        stats.println("Passenger Delivery Summary");
        stats.println("Delivered: " + delivered);
        stats.println("Stranded: " + stranded);
        stats.println("Not_injected: " + notInjected);
        stats.println("Total: " + (allPassengers.size() + passengerPool.size()));
        
        if (notInjected == 0 && stranded == 0) {
            stats.println();
            stats.println("Performance Stats");
            stats.format("Average_delivery_time: %.3f", average);
            stats.println();
            stats.format("Maximum_delivery_time:  %.3f", maxDeliveryTime.getFracSeconds());
            stats.println();
            stats.format("Performance_score:  %.3f", (4 * average + (maxDeliveryTime.getFracSeconds())));
            stats.println();
        } else {
            stats.println();
            stats.println("Average_delivery_time: n/a");
            stats.println("Maximum_delivery_time: n/a");
            stats.println("Performance_score: " + Integer.MAX_VALUE);            
        }

        stats.close();
        return output.toString();
    }


    private static String toString(Passenger p) {
        return String.format(
                "%s: start=(%d %5s) destination=(%d %5s): " +
                "injected at %11f sec, \tdelivered at %11f sec -> serviced in %f sec",
                p.toString(),
                p.getStartFloor(), p.getStartHallway(),
                p.getDestinationFloor(), p.getDestinationHallway(),
                p.getStartTime().getFracSeconds(),
                p.isDelivered() ? (p.getArrivalTime().getFracSeconds())
                : Double.NaN,
                p.isDelivered() ? (p.getArrivalTime().getFracSeconds() - p.getStartTime().getFracSeconds())
                : Double.NaN);
    }

    private void log(String s) {
        if (verbose) {
            Harness.log("PassengerInjector", s);
        }
    }
}
