package simulator.elevatormodules.passengers;

import jSimPack.SimTime;
import java.util.ArrayList;
import java.util.List;
import simulator.framework.Harness;

/**
 * Keep track of satisfaction deductions.  100.0 is a perfect score.
 */
public class PassengerSatisfaction {

    private class Deduction{
        final double value;
        final String comment;
        final SimTime time;

        public Deduction(double value, String comment, SimTime time) {
            this.value = value;
            this.comment = comment;
            this.time = time;
        }
    }

    private List<Deduction> deductions;
    private double score;

    public PassengerSatisfaction() {
        score = 100;
        deductions = new ArrayList<Deduction>();
    }

    /**
     * 
     * @param reduction a double between 0 and 1.  The current score will be reduced by multiplying by this value
     * @param comment Description of the reason for the deduction
     */
    public void addDeduction(double reduction, String comment) {
        this.score *= reduction;
        deductions.add(new Deduction(reduction, comment, Harness.getTime()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  Satisfaction score: ");
        sb.append(score);
        sb.append("\n");
        for (Deduction d : deductions) {
            sb.append(String.format("    @%4.9f: %2.2f %s\n", d.time.getFracSeconds(), d.value, d.comment));
        }
        return sb.toString();
    }

    public double getScore() {
        return score;
    }
}
