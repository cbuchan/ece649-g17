/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules;

/**
 * This exception is thrown when the elevator violates a safety condition.
 * @author Justin Ray
 */
public class SafetyViolationException extends RuntimeException {

    public static final long serialVersionUID = 1L;
    public SafetyViolationException(String msg) {
        super(msg);
    }
    public SafetyViolationException(Throwable ex) {
        super(ex);
    }
    
}
