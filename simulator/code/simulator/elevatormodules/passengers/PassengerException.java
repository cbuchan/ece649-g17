/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.elevatormodules.passengers;

/**
 * Exception thrown by PassengerInfo if the info is incorrect.
 * @author justinr2
 */
public class PassengerException extends Exception {
    private final static long serialVersionUID = 0;
    public PassengerException(String message) {
        super(message);
    }
    public PassengerException(Throwable cause) {
        super(cause);
    }
        public PassengerException(String message, Throwable cause) {
        super(message, cause);
    }
}
