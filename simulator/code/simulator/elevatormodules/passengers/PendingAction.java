package simulator.elevatormodules.passengers;

/**
 * A storage class for an action.  The main purpose of this class is to ensure
 * that the current action is canceled when a new action is set.
 *
 * This class uses generics so that objects can require a more specific type.
 * See the DoorAction and CallAction classes in the Passenger class for examples.
 */
class PendingAction <T extends PassengerAction> {

    private PassengerAction theAction = null;

    /**
     * Cancel the current action (if there is one) and save the new action.  
     *
     * @param newAction
     */
    protected void set(T newAction) {
        if (theAction != null) {
            theAction.cancel();
        }
        theAction = newAction;
    }

    /**
     * 
     * @return the current action
     */
    public PassengerAction get() {
        return theAction;
    }

    /**
     * cancel the current action
     */
    public void cancel() {
        theAction.cancel();
    }
}
