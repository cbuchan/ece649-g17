/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator.elevatorcontrol;

import simulator.elevatormodules.*;
import simulator.framework.Hallway;
import simulator.framework.RuntimeMonitor;
import simulator.payloads.AtFloorPayload.ReadableAtFloorPayload;
import simulator.payloads.DriveSpeedPayload.ReadableDriveSpeedPayload;

/**
 * This example monitor shows how to use the RuntimeMonitor hooks to check for
 * fast speed between floors.  You will need to implement additional checks
 * to fulfill the project 12 requirements.
 *
 * See the documentation of simulator.framework.RuntimeMonitor for more details.
 *
 * @author Justin Ray
 */
public class SampleDispatcherMonitor extends RuntimeMonitor {

    protected int currentFloor = MessageDictionary.NONE;
    protected int lastStoppedFloor = MessageDictionary.NONE;
    protected boolean fastSpeedReached = false;

    public SampleDispatcherMonitor() {
        //initialization goes here
    }

    public void timerExpired(Object callbackData) {
        //implement time-sensitive behaviors here
    }

    @Override
    public void receive(ReadableAtFloorPayload msg) {
        updateCurrentFloor(msg);
    }

    @Override
    public void receive(ReadableDriveSpeedPayload msg) {
        checkFastSpeed(msg);
    }

    /**
     * Warn if the drive was never commanded to fast when fast speed could be 
     * used.
     * @param msg
     */
    private void checkFastSpeed(ReadableDriveSpeedPayload msg) {
        if (msg.speed() == 0 && currentFloor != MessageDictionary.NONE) {
            //stopped at a floor
            if (lastStoppedFloor != currentFloor) {
                //we've stopped at a new floor
                if (fastSpeedAttainable(lastStoppedFloor, currentFloor)) {
                    //check and see if the drive was ever reached fast
                    if (!fastSpeedReached) {
                        warning("The drive was not commanded to FAST on the trip between " + lastStoppedFloor + " and " + currentFloor);
                    }
                }
                //now that the check is done, set the lastStoppedFloor to this floor
                lastStoppedFloor = currentFloor;
                //reset fastSpeedReached
                fastSpeedReached = false;
            }
        }
        if (msg.speed() > DriveObject.SlowSpeed) {
            //if the drive exceeds the Slow Speed, the drive must have been commanded to fast speed.
            fastSpeedReached = true;
        }
    }

    /*--------------------------------------------------------------------------
     * Utility and helper functions
     *------------------------------------------------------------------------*/
    /**
     * Computes whether fast speed is attainable.  In general, it is attainable 
     * between any two floors.
     * 
     * @param startFloor
     * @param endFloor
     * @return true if Fast speed can be commanded between the given floors, otherwise false
     */
    private boolean fastSpeedAttainable(int startFloor, int endFloor) {
        //fast speed is attainable between all floors
        if (startFloor == MessageDictionary.NONE || endFloor == MessageDictionary.NONE) {
            return false;
        }
        if (startFloor != endFloor) {
            return true;
        }
        return false;
    }


    private void updateCurrentFloor(ReadableAtFloorPayload lastAtFloor) {
        if (lastAtFloor.getFloor() == currentFloor) {
            //the atFloor message is for the currentfloor, so check both sides to see if they a
            if (!atFloors[lastAtFloor.getFloor()-1][Hallway.BACK.ordinal()].value() && !atFloors[lastAtFloor.getFloor()-1][Hallway.FRONT.ordinal()].value()) {
                //both sides are false, so set to NONE
                currentFloor = MessageDictionary.NONE;
            }
            //otherwise at least one side is true, so leave the current floor as is
        } else {
            if (lastAtFloor.value()) {
                currentFloor = lastAtFloor.getFloor();
            }
        }
    }

    @Override
    protected String[] summarize() {
        return new String[0]; //nothing to summarize
    }




}
