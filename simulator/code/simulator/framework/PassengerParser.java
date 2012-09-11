package simulator.framework;

import jSimPack.SimTime;

import java.text.ParseException;
import java.util.*;
import simulator.elevatormodules.passengers.Passenger;
import simulator.elevatormodules.passengers.PassengerControl;
import simulator.elevatormodules.passengers.PassengerInfo;


/**
 * Parses the -pf file and creates passenger objects accordingly.
 * @author justinr2
 */
public class PassengerParser implements Parser {

    private boolean verbose;
    private final List<Passenger> passengers;
    private final PassengerControl pc;
    private SimTime lastInjectionTime = SimTime.ZERO;
    private final Random random; //use this to generate a repeatable sequence of seeds for individual passengers

    public PassengerParser(String filename, long randomSeed, PassengerControl pc, boolean verbose) {
        this.verbose = verbose;
        this.pc = pc;
        passengers = new ArrayList<Passenger>();
        random = new Random(randomSeed);
        new FileTokenizer(filename, verbose, this).parseFile();
    }

    public void parse(String[] words, FileTokenizer sourceFT) throws ParseException {
        PassengerInfo pi;
        try {
            pi = new PassengerInfo(words, random.nextLong(), lastInjectionTime);
        } catch (ParseException ex) {
            throw new ParseException(sourceFT.lineMessage(ex.getMessage()), ex.getErrorOffset());
        }
        if (pi.injectionTime.isAfter(lastInjectionTime)) {
            lastInjectionTime = pi.injectionTime;
        }
        passengers.add(new Passenger(pc, pi, verbose));
    }

    /**
     * @return the latest passenger injection time
     */
    public SimTime getLastInjectionTime() {
        return lastInjectionTime;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    private void log(String s) {
        if (verbose) {
            Harness.log("PassengerParser", s);
        }
    }
}
